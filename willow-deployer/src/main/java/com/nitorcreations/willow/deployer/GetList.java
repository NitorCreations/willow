package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.ENV_DEPLOYER_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.inject.Singleton;

import org.hyperic.sigar.SigarException;

@Singleton
public class GetList extends DeployerControl {
  public static void main(String[] args) {
    injector.getInstance(GetList.class).doMain(args);
  }

  public void doMain(String[] args) {
    try {
      List<Long> firstPids;
      if (args.length > 0) {
        firstPids = new ArrayList<>();
        for (String next : args) {
          long nextPid = findOldDeployerPid(next);
          if (nextPid > 0) {
            firstPids.add(nextPid);
          }
        }
      } else {
        firstPids = findOldDeployerPids();
      }
      for (long next : firstPids) {
        System.out.println(getOldDeployerName(next));
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, "Failed to connect to deployer " + deployerName);
    }
  }
  protected String getOldDeployerName(long pid) throws SigarException {
    return sigar.getProcEnv(pid, ENV_DEPLOYER_NAME);
  }
}
