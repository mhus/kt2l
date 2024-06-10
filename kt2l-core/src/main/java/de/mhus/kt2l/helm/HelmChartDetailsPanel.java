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
package de.mhus.kt2l.helm;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import de.mhus.commons.io.Zip;
import de.mhus.commons.tools.MJson;
import de.mhus.commons.yaml.MYaml;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import io.kubernetes.client.openapi.models.V1Secret;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class HelmChartDetailsPanel extends VerticalLayout implements DeskTabListener {

    private final Core core;
    private final Cluster cluster;
    private final V1Secret resource;
    private final Map<String, String> files = new LinkedHashMap<>();
    private String values;
    private String info;
    private String chartMetadata;
    private TextArea content;

    public HelmChartDetailsPanel(Core core, Cluster cluster, V1Secret resource) throws IOException {
        this.core = core;
        this.cluster = cluster;
        this.resource = resource;

        extractData();

    }

    private void extractData() throws IOException {
        var data = Base64.getDecoder().decode(resource.getData().get("release"));
        var rawStream = new ByteArrayOutputStream();
        Zip.builder().srcStream(new ByteArrayInputStream(data)).dstStream(rawStream).build().ungzip();
        var jsonStr = new String(rawStream.toByteArray());
        var json = MJson.load(jsonStr);

        info = MYaml.toYaml(json.get("info")).toString();

        var chart = json.get("chart");
        chartMetadata = MYaml.toYaml(chart.get("metadata")).toString();

        var templates = chart.get("templates");
        for (int i = 0; i < templates.size(); i++) {
            var template = templates.get(i);
            var name = template.get("name").asText();
            var dataStr = template.get("data").asText();
            var dataBytes = Base64.getDecoder().decode(dataStr);
            files.put(name, new String(dataBytes));
        }

        values = MYaml.toYaml(chart.get("values")).toString();

        var filez = chart.get("files");
        for (int i = 0; i < filez.size(); i++) {
            var template = filez.get(i);
            var name = template.get("name").asText();
            var dataStr = template.get("data").asText();
            var dataBytes = Base64.getDecoder().decode(dataStr);
            files.put(name, new String(dataBytes));
        }

    }

    @Override
    public void tabInit(DeskTab deskTab) {
        var split = new SplitLayout();
        split.setSizeFull();
        add(split);

        var tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.setHeightFull();
        tabs.setWidth("240px");

        split.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        split.addToPrimary(tabs);

        content = new TextArea();
        content.setReadOnly(true);
        content.addClassName("monotext");
        content.setSizeFull();
        split.addToSecondary(content);

        tabs.add(new TextTab("Info", info));
        tabs.add(new TextTab("Chart", chartMetadata));
        tabs.add(new TextTab("Values", values));

        files.forEach((name, data) -> {
            var tab = new TextTab(name, data);
            tabs.add(tab);
        });


        tabs.addSelectedChangeListener(e -> {
            if (e.getSelectedTab() instanceof TextTab textTab) {
                content.setValue(textTab.text);
            } else
                content.setValue("");
        });


        setSizeFull();
        getElement().getStyle().set("overflow", "hidden");
        setPadding(false);
        setSpacing(false);
        setMargin(false);


        tabs.setSelectedTab(tabs.getTabAt(0));

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

    private class TextTab extends Tab {

        String text;

        public TextTab(String label) {
            super(label);
        }

        public TextTab(String title, String text) {
            super(title);
            this.text = text;
        }
    }
}
