package de.mhus.kt2l.kscript;

import de.mhus.commons.tree.IReadonly;
import io.kubernetes.client.Attach;
import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.models.V1EphemeralContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CmdDebug extends Cmd {

    @Override
    void run(RunContext context, IReadonly args) throws Exception {

        var useStdin = args.getBoolean("stdin", true);
        var useTty = args.getBoolean("tty", true);
        var execStr = args.getString("exec").orElse(null);

        var pod = context.getPod();
        if (pod.getSpec().getEphemeralContainers() == null)
            pod.getSpec().setEphemeralContainers(new ArrayList<>());
        var container = new V1EphemeralContainer();
        var name = "debugger-" + UUID.randomUUID().toString();
        container.setName(name);
        container.setImage(args.getString("image", "busybox"));
        container.setCommand(List.of(args.getString("cmd", "sh").split(",")));
        container.setStdin(useStdin);
        container.setTty(useTty);
        pod.getSpec().getEphemeralContainers().add(container);
        context.getApiProvider().getCoreV1Api().replaceNamespacedPodEphemeralcontainers(pod.getMetadata().getName(), pod.getMetadata().getNamespace(), pod, null, null, null, null);

        context.getProperties().setString(scope + ".container", name);
        context.addContent("--- New Container: " + name);

        if (execStr == null) {
            Attach attach = new Attach(context.getApiProvider().getClient());
            var result = attach.attach(
                    context.getPod(),
                    context.getProperties().getString(RunCompiler.PROP_CONTAINER).orElse(""),
                    useStdin, useTty);

            context.setScope(scope, new AttachScope(context, result));
        } else {
            var cmdArray = execStr.split(",");
            Exec exec = new Exec(context.getApiProvider().getClient());
            var proc = exec.exec(
                    context.getPod(),
                    cmdArray,
                    context.getProperties().getString(RunCompiler.PROP_CONTAINER).orElse(null),
                    useStdin, useTty);

            context.setScope(scope, new ExecScope(context, proc));
        }

    }

    @Override
    protected Block init() throws Exception {
        return block;
    }
}
