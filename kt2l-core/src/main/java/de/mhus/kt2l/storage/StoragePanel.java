package de.mhus.kt2l.storage;

import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamReceiver;
import com.vaadin.flow.server.StreamResource;
import de.mhus.commons.tools.MDate;
import de.mhus.commons.tools.MFile;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tools.MThread;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.core.UiUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.UUID;

@Slf4j
public class StoragePanel extends VerticalLayout implements DeskTabListener {


    @Autowired
    private StorageService storageService;

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

        });
        itemDelete = menuBar.addItem("Delete", e -> {
            deleteSelected();
        });
        itemOpen = menuBar.addItem("Open", e -> {
            if (!storageService.open(selectedCurrent))
                UiUtil.showErrorNotification("Can't open file locally");
        });
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

        grid = new Grid<StorageFile>();
        grid.setSizeFull();

        grid.addColumn(StorageFile::getName).setHeader("Name").setSortable(true);
        grid.addColumn(s -> s.getSize() <= 0 ? "" : MString.toByteDisplayString(s.getSize())).setHeader("Size").setSortable(true);
        grid.addColumn(s -> s.isDirectory() ? "dir" : "file").setHeader("Type").setSortable(true);
        grid.addColumn(s -> s.getModified() <= 0 ? "" : MDate.toIso8601(s.getModified())).setHeader("Modified").setSortable(true);

        grid.addItemDoubleClickListener(e -> {
            var selected = e.getItem();
            if (selected == null) return;
            if (selected.isDirectory()) {
                selectedDirectory = selected;
                showSelectedFiles();
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
            } else {
                itemOpen.setEnabled(true);
                itemDownload.setEnabled(true);
                itemDelete.setEnabled(true);

                selectedCurrent = selected.get();
            }
            showBreadcrumb();
        });

        add(downloads, breadCrumb,menuBar, grid);

        selectedDirectory = new StorageFile(storageService.getStorage(), "", "", true, -1, -1);
        showSelectedFiles();
        showBreadcrumb();

//        try {
//            lists[0].setItems(storageService.getStorage().listFiles("/"));
//        } catch (Exception e) {
//            LOGGER.error(e.getMessage(), e);
//        }

        setSizeFull();
    }

    private void downloadSelected() {
        try {

            if (selectedCurrent == null) return;
            if (selectedCurrent.isDirectory()) {
//                FileDownloadWrapper link = new FileDownloadWrapper(selectedCurrent.getName() + ".zip", () -> textField.getValue().getBytes());
            } else {
                var ui = getUI();
                var stream = selectedCurrent.getStorage().openFile(selectedCurrent.getPath()).getStream();
                var closeActionStream = new InputStream() {
                    public FileDownloadWrapper download;

                    @Override
                    public int read() throws IOException {
                        return stream.read();
                    }

                    public int read(byte[] b, int off, int len) throws IOException {
                        return stream.read(b, off, len);
                    }

                    @Override
                    public void close() throws IOException {
                        stream.close();
                        ui.get().access(() -> {
                            downloads.remove(download);
                        });
                    }
                };
                FileDownloadWrapper download = new FileDownloadWrapper(new StreamResource(selectedCurrent.getName(), () -> closeActionStream));
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
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            UiUtil.showErrorNotification("Can't prepare file for download",e);
        }
    }

    private void deleteSelected() {
    }

    private void showBreadcrumb() {
        breadCrumb.removeAll();
        var path = MFile.normalizePath(selectedDirectory.getPath()).split("/");
        var currentPath = "";
        var cnt = 0;
        for (String pathElement : path) {
            currentPath += "/" + pathElement;
            if (cnt != 0) {
                breadCrumb.add(new Text("/"));
            }
            if (cnt == path.length-1 && !selectedDirectory.isDirectory()) {
                breadCrumb.add(new Text(pathElement));
            } else {
                var item = new Button(cnt == 0 ? "#" : pathElement);
                item.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                final var selected = new StorageFile(selectedDirectory.getStorage(), currentPath, pathElement, true, -1, -1);
                item.addClickListener(e -> {
                    selectedDirectory = selected;
                    showSelectedFiles();
                    showBreadcrumb();
                });
                breadCrumb.add(item);
            }
            cnt++;
        }
    }

    private void showSelectedFiles() {
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

    @Override
    public void tabShortcut(ShortcutEvent event) {

    }
}
