package org.myPaper.vm.instances;

import org.cloudbus.cloudsim.vms.Vm;
import org.myPaper.vm.VmInstanceAbstract;

public class VmInstance1_A1_Medium extends VmInstanceAbstract {

    @Override
    protected int getCpu() {
        return 1;
    }

    @Override
    protected int getMIPS() {
        return MIPS_1;
    }

    @Override
    protected double getMemory() {
        return convertGigaToMega(2);
    }

    /**
     * Instance Type: a1.medium <a href="https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/Instances.html">aws</a><br/>
     * <table>
     *     <thead>
     *         <tr>
     *             <td>Type</td>
     *             <td>CPU</td>
     *             <td>MIPS</td>
     *             <td>Memory (GB)</td>
     *         </tr>
     *     </thead>
     *     <tbody>
     *         <tr>
     *             <td>a1.medium</td>
     *             <td>1</td>
     *             <td>{@link VmInstanceAbstract#MIPS_1}</td>
     *             <td>2</td>
     *         </tr>
     *     </tbody>
     * </table>
     * @return new vm instance
     */
    @Override
    public Vm createVm() {
        return newVm();
    }
}
