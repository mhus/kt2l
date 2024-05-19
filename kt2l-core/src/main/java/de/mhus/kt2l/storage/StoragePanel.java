package de.mhus.kt2l.storage;

import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

@Slf4j
public class StoragePanel extends VerticalLayout implements DeskTabListener {


    @Autowired
    private StorageService storageService;

    private ListBox<StorageFile>[] lists;
    private HorizontalLayout listPanel;
    private MenuItem itemOpen;
    private MenuItem itemDownload;
    private MenuItem itemDelete;
    private StorageFile selectedFile;

    @Override
    public void tabInit(DeskTab deskTab) {

        listPanel = new HorizontalLayout();
        listPanel.setSizeFull();
        listPanel.setMargin(false);
        listPanel.setPadding(false);
        listPanel.setSpacing(false);

        lists = new ListBox[5];
        for (int i = 0; i < lists.length; i++) {
            final int finalIndex = i;
            lists[i] = new ListBox<>();
            lists[i].setWidth("200px");
            lists[i].setHeightFull();
            lists[i].setItemLabelGenerator(StorageFile::getName);
            if (i == 3)
                lists[i].setItemLabelGenerator(s -> MString.afterIndex(s.getName(), '_')  );
            if (i < 4) {
                lists[i].addValueChangeListener(e -> {
                    selectedFile = e.getValue();
                    if (selectedFile == null) {
                        itemOpen.setEnabled(false);
                        itemDownload.setEnabled(false);
                        itemDelete.setEnabled(false);
                        lists[finalIndex + 1].clear();
                        lists[finalIndex + 1].setItems();
                    } else {
                        itemOpen.setEnabled(true);
                        itemDownload.setEnabled(true);
                        itemDelete.setEnabled(true);
                        if (e.getValue().isDirectory()) {
                            try {
                                var files = new LinkedList<>(e.getValue().getStorage().listFiles(e.getValue()));
                                files.sort((a,b) -> -a.getName().compareTo(b.getName()));
                                lists[finalIndex + 1].setItems(files);
                                for (int j = finalIndex + 2; j < lists.length; j++) {
                                    lists[j].clear();
                                    lists[j].setItems();
                                }
                            } catch (Exception ex) {
                                LOGGER.error(ex.getMessage(), ex);
                            }
                        } else {
                            lists[finalIndex + 1].clear();
                            lists[finalIndex + 1].setItems();
                        }
                    }
                    try {
                        for (int j = finalIndex + 2; j < lists.length; j++) {
                            lists[j].clear();
                            lists[j].setItems();
                        }
                    } catch (Exception ex) {
                        LOGGER.error(ex.getMessage(), ex);
                    }

                });
            } else {
                lists[i].setWidth("100%");
                lists[i].addValueChangeListener(e -> {
                    selectedFile = e.getValue();
                    if (selectedFile == null) {
                        itemOpen.setEnabled(false);
                        itemDownload.setEnabled(false);
                        itemDelete.setEnabled(false);
                    } else {
                        itemOpen.setEnabled(true);
                        itemDownload.setEnabled(true);
                        itemDelete.setEnabled(true);
                    }
                });
            }
            listPanel.add(lists[i]);
        }

        var menuBar = new MenuBar();
        menuBar.setWidthFull();
        itemDelete = menuBar.addItem("Delete", e -> {
        });
        itemOpen = menuBar.addItem("Open", e -> {
        });
        itemDownload = menuBar.addItem("Download", e -> {
        });

        add(menuBar, listPanel);

        try {
            lists[0].setItems(storageService.getStorage().listFiles("/"));
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
