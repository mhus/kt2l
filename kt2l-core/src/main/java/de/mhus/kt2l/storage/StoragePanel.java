/*
 * kt2l-core - kt2l core implementation
 * Copyright © 2024 Mike Hummel (mh@mhus.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l.storage;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;
import de.mhus.commons.io.PipedStream;
import de.mhus.commons.tools.MDate;
import de.mhus.commons.tools.MFile;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tools.MThread;
import de.mhus.kt2l.aaa.SecurityContext;
import de.mhus.kt2l.aaa.SecurityService;
import de.mhus.kt2l.aaa.UsersConfiguration;
import de.mhus.kt2l.config.ViewsConfiguration;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.ui.ProgressDialog;
import de.mhus.kt2l.ui.UiUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipOutputStream;

@Slf4j
public class StoragePanel extends VerticalLayout implements DeskTabListener {


    @Autowired
    private StorageService storageService;

    @Autowired
    private ViewsConfiguration viewsConfiguration;

    @Autowired
    private SecurityService securityService;

//    private ListBox<StorageFile>[] lists;
//    private HorizontalLayout listPanel;
    private MenuItem itemOpen;
    private MenuItem itemDownload;
    private MenuItem itemDelete;
    private StorageFile selectedDirectory;
    private HorizontalLayout breadCrumb;
    private Grid<StorageFile> grid;
    private StorageFile selectedCurrent;
    private HorizontalLayout downloads;

    @Override
    public void tabInit(DeskTab deskTab) {

        var menuBar = new MenuBar();
        menuBar.setWidthFull();
        menuBar.addItem(VaadinIcon.REFRESH.create(),e -> {
            updateSelectedFiles();
        });
        itemDelete = menuBar.addItem("Delete", e -> {
            deleteSelected();
        });
        if (securityService.hasRole(UsersConfiguration.ROLE.LOCAL)) {
            itemOpen = menuBar.addItem("Open", e -> {
                if (!storageService.open(selectedCurrent))
                    UiUtil.showErrorNotification("Can't open file locally");
            });
        }
        itemDownload = menuBar.addItem("Download", e -> {
            downloadSelected();
        });

//        add(menuBar, listPanel);

        downloads = new HorizontalLayout();
        downloads.setMargin(false);
        downloads.setPadding(false);

        breadCrumb = new HorizontalLayout();
        breadCrumb.setWidthFull();
        // breadCrumb.setHeight("60px");
        breadCrumb.setMargin(false);
        breadCrumb.setPadding(false);
        breadCrumb.setSpacing(true);
        breadCrumb.addClassName("breadcrumb");

        grid = new Grid<StorageFile>();
        grid.setSizeFull();

        grid.addColumn(StorageFile::getName).setHeader("Name").setSortProperty("name");
        grid.addColumn(s -> s.getSize() <= 0 ? "" : MString.toByteDisplayString(s.getSize())).setHeader("Size").setSortProperty("size");
        grid.addColumn(s -> s.isDirectory() ? "dir" : "file").setHeader("Type").setSortProperty("type");
        grid.addColumn(s -> s.getModified() <= 0 ? "" : MDate.toIso8601(s.getModified())).setHeader("Modified").setSortProperty("modified");
        grid.setColumnReorderingAllowed(true);
        grid.getColumns().forEach(col -> {
            col.setAutoWidth(true);
            col.setResizable(true);
        });


        grid.addItemDoubleClickListener(e -> {
            var selected = e.getItem();
            if (selected == null) return;
            if (selected.isDirectory()) {
                selectedDirectory = selected;
                updateSelectedFiles();
                selectedCurrent = null;
                itemOpen.setEnabled(false);
                itemDownload.setEnabled(false);
                itemDelete.setEnabled(false);
            }
        });
        grid.addSelectionListener(e -> {
            var selected = e.getFirstSelectedItem();
            if (selected.isEmpty()) {
                itemOpen.setEnabled(false);
                itemDownload.setEnabled(false);
                itemDelete.setEnabled(false);
                selectedCurrent = null;
            } else {
                itemOpen.setEnabled(true);
                itemDownload.setEnabled(true);
                itemDelete.setEnabled(true);
                selectedCurrent = selected.get();
            }
            updateBreadcrumb();
        });

        add(breadCrumb,menuBar, grid, downloads);

        selectedDirectory = new StorageFile(storageService.getStorage(), "", "", true, -1, -1);
        updateSelectedFiles();
        updateBreadcrumb();

//        try {
//            lists[0].setItems(storageService.getStorage().listFiles("/"));
//        } catch (Exception e) {
//            LOGGER.error(e.getMessage(), e);
//        }

        setSizeFull();
        setPadding(false);
        setMargin(false);
    }

    private void downloadSelected() {
        try {

            if (selectedCurrent == null) return;
            InputStream inputStream = null;
            if (selectedCurrent.isDirectory()) {
                List<StorageFile> list = new LinkedList<>();
                collectFiles(list, selectedCurrent);

                PipedStream pipe = new PipedStream(viewsConfiguration.getConfig("storage").getInt("downloadPipeSize", 1024*1024*1024));
                pipe.setReadTimeout(viewsConfiguration.getConfig("storage").getInt("downloadPipeReadTimeout", 6000));
                pipe.setWriteTimeout(viewsConfiguration.getConfig("storage").getInt("downloadPipeWriteTimeout", 6000));
                inputStream = pipe.getIn();

                ZipOutputStream zos = new ZipOutputStream(pipe.getOut());
//                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("/tmp/download.zip"));

                ProgressDialog dialog = new ProgressDialog();
                dialog.setMax(list.size());
                dialog.open();
                var ui = getUI();

                Thread.startVirtualThread(() -> {
                    try {
                        int cnt = 0;
                        for (StorageFile file : list) {
                            LOGGER.debug("Zip for Download: " + file.getPathAndName());
                            ui.get().access(() -> dialog.next(file.getPathAndName()));
                            try (var stream = file.getStorage().openFile(file.getPathAndName()).getStream()){
                                zos.putNextEntry(new java.util.zip.ZipEntry(file.getPathAndName()));
                                MFile.copyFile(stream, zos);
                                zos.flush();
                                zos.closeEntry();
                            }
                        }
                        zos.finish();
                        zos.flush();
                        zos.close();
                        pipe.getOut().flush();
                        pipe.getOut().close();
                        while (!pipe.isInputClosed()) {
                            MThread.sleep(1000);
                            ui.get().access(() -> dialog.setProgressDetails("Wait for " + pipe.getBufferedSize() + " bytes"));
                        }
                        ui.get().access(() -> dialog.close());
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                });

//                FileDownloadWrapper link = new FileDownloadWrapper(selectedCurrent.getName() + ".zip", () -> textField.getValue().getBytes());
            } else {
                inputStream = selectedCurrent.getStorage().openFile(selectedCurrent.getPathAndName()).getStream();
            }

            final var ui = getUI();
            final var finalInputStream = inputStream;
            var closeActionStream = new InputStream() {
                public FileDownloadWrapper download;

                @Override
                public int read() throws IOException {
                    return finalInputStream.read();
                }

                public int read(byte[] b, int off, int len) throws IOException {
                    return finalInputStream.read(b, off, len);
                }

                @Override
                public void close() throws IOException {
                    finalInputStream.close();
                    ui.get().access(() -> {
                        downloads.remove(download);
                    });
                }
            };
            FileDownloadWrapper download = new FileDownloadWrapper(new StreamResource(selectedCurrent.getName() + (selectedCurrent.isDirectory() ? ".zip" : ""), () -> closeActionStream));
            closeActionStream.download = download;
            download.setText("[" + selectedCurrent.getName() + "]");
            var id = UUID.randomUUID().toString().replace("-", "");
            download.setId(id);
            downloads.add(download);
            Thread.startVirtualThread(() -> {
                try {
                    MThread.sleep(200);
                    ui.get().access(() ->
                            download.getElement().executeJs("$('#"+id+" a')[0].click();"));

                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            });

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            UiUtil.showErrorNotification("Can't prepare file for download",e);
        }
    }

    private void collectFiles(List<StorageFile> list, StorageFile parent) {
        if (!parent.isDirectory()) {
            list.add(parent);
            return;
        }
        try {
            var files = storageService.getStorage().listFiles(parent);
            for (StorageFile file : files) {
                if (file.isDirectory()) {
                    collectFiles(list, file);
                } else {
                    list.add(file);
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void collectDirectories(List<StorageFile> list, StorageFile parent) {
        if (!parent.isDirectory()) {
            return;
        }
        try {
            var files = storageService.getStorage().listFiles(parent);
            for (StorageFile file : files) {
                if (file.isDirectory()) {
                    collectDirectories(list, file);
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        list.add(parent);
    }

    private void deleteSelected() {
        if (selectedCurrent == null) return;
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Delete File");
        dialog.add(new Text("Do you really want to delete the file: " + selectedCurrent.getName()));
        Button ok = new Button("Delete", e -> {
            dialog.close();
            doDeleteSelected();
        });
        Button cancel = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(ok, cancel);
        dialog.open();

    }

    private void doDeleteSelected() {
        try {
            List<StorageFile> list = new LinkedList<>();
            collectFiles(list, selectedCurrent);
            collectDirectories(list, selectedCurrent);
            ProgressDialog dialog = new ProgressDialog();
            dialog.setMax(list.size());
            dialog.open();

            var sc = SecurityContext.create();
            var ui = getUI();
            Thread.startVirtualThread(() -> {
                try (var sce = sc.enter()) {
                    int cnt = 0;
                    for (StorageFile file : list) {
                        LOGGER.debug("Delete: " + file.getPathAndName());
                        ui.get().access(() -> dialog.next(file.getPathAndName()));
                        storageService.getStorage().delete(file);
                    }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                    ui.get().access(() -> {
                        UiUtil.showErrorNotification("Can't delete file", e);
                        dialog.close();
                    });
                }
                ui.get().access(() -> dialog.close());
                ui.get().access(() -> updateSelectedFiles());
            });
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            UiUtil.showErrorNotification("Can't delete file", e);
        }
    }

    private void updateBreadcrumb() {
        breadCrumb.removeAll();
        var selectedNow = selectedCurrent == null ? selectedDirectory : selectedCurrent;
        var path = MFile.normalizePath(selectedNow.getPathAndName()).split("/");
        var currentPath = "";
        var cnt = 0;
        for (String pathElement : path) {
            if (cnt != 0) {
                breadCrumb.add(new Text("/"));
            }
            if (cnt == path.length-1 && !selectedNow.isDirectory()) {
                var div = new Div(pathElement);
                div.addClassName("file");
                breadCrumb.add(div);
            } else {
                var item = new Button(cnt == 0 ? "#" : pathElement);
                item.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                final var selected = new StorageFile(selectedNow.getStorage(), currentPath, pathElement, true, -1, -1);
                item.addClickListener(e -> {
                    selectedDirectory = selected;
                    selectedCurrent = null;
                    updateSelectedFiles();
                    updateBreadcrumb();
                });
                breadCrumb.add(item);
            }
            currentPath += "/" + pathElement;
            cnt++;
        }
    }

    private void updateSelectedFiles() {
        try {
            var files = new LinkedList<>(storageService.getStorage().listFiles(selectedDirectory));
            files.sort((a,b) -> -a.getName().compareTo(b.getName()));
            grid.setItems(files);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabUnselected() {

    }

    @Override
    public void tabDestroyed() {

    }

    @Override
    public void tabRefresh(long counter) {

    }

    public void showFile(StorageFile file) {
        if (file.isDirectory()) {
            selectedDirectory = file;
            selectedCurrent = null;
        } else {
            selectedDirectory = new StorageFile(file.getStorage(), file.getPath(), "", true, -1, -1);
            selectedCurrent = file;
        }
        updateSelectedFiles();
        if (selectedCurrent != null)
            grid.select(selectedCurrent);
        updateBreadcrumb();
    }
}
