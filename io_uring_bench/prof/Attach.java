import com.sun.tools.attach.VirtualMachine;
public class Attach {
    public static void main(String[] a) throws Exception {
        VirtualMachine vm = VirtualMachine.attach(a[0]);   // pid
        try { vm.loadAgentPath(a[1], a[2]); }               // soPath, options
        finally { vm.detach(); }
        System.out.println("agent op ok: " + a[2]);
    }
}
