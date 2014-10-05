package com.nitorcreations.willow.deployer;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class Environment {
	public interface WinLibC extends Library {
		public int _putenv(String name);
	}
	public interface LinuxLibC extends Library {
		public int setenv(String name, String value, int overwrite);
		public int unsetenv(String name);
	}
	static public class POSIX {
		static Object libc;
		static {
			if (System.getProperty("os.name").toLowerCase().contains("win")) {
				libc = Native.loadLibrary("msvcrt", WinLibC.class);
			} else {
				libc = Native.loadLibrary("c", LinuxLibC.class);
			}
		}

		public int setenv(String name, String value, int overwrite) {
			if (libc instanceof LinuxLibC) {
				return ((LinuxLibC)libc).setenv(name, value, overwrite);
			}
			else {
				return ((WinLibC)libc)._putenv(name + "=" + value);
			}
		}

		public int unsetenv(String name) {
			if (libc instanceof LinuxLibC) {
				return ((LinuxLibC)libc).unsetenv(name);
			}
			else {
				return ((WinLibC)libc)._putenv(name + "=");
			}
		}
	}
	static POSIX libc = new POSIX();
}