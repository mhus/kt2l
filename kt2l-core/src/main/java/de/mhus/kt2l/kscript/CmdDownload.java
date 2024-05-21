package de.mhus.kt2l.kscript;

import de.mhus.commons.tools.MFile;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tree.IReadonly;
import io.kubernetes.client.Copy;

public class CmdDownload extends Cmd {
    @Override
    void run(RunContext context, IReadonly args) throws Exception {
        var from = args.getString("from").orElseThrow(() -> new IllegalArgumentException("from is required"));
        var to = args.getString("to").orElseGet(() -> MString.afterLastIndex(from, '/'));

        Copy copy = new Copy(context.getApiProvider().getClient());
        try (var is = copy.copyFileFromPod(
                context.getPod(),
                context.getProperties().getString(RunCompiler.PROP_CONTAINER).orElse(null),
                from
        )) {
            try (var os = context.createFileStream(to)) {
                MFile.copyFile(is, os);
            }
        }
    }

    @Override
    protected Block init() throws Exception {
        return block;
    }
}
