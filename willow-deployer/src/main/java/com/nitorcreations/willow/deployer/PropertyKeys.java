package com.nitorcreations.willow.deployer;

public interface PropertyKeys {
	public static final String PROPERTY_KEY_LAUNCH_METHOD                   = "deployer.launch.method";
	public static final String PROPERTY_KEY_PREFIX_LAUNCH_ARGS              = "deployer.launch.arg";
	public static final String PROPERTY_KEY_PREFIX_DOWNLOAD_URL             = "deployer.download.url";
	public static final String PROPERTY_KEY_PREFIX_DOWNLOAD_FINALPATH       = "deployer.download.finalpath";
	public static final String PROPERTY_KEY_PREFIX_DOWNLOAD_ARTIFACT        = "deployer.download.artifact";
	public static final String PROPERTY_KEY_PREFIX_EXTRACT_INTERPOLATE_GLOB = "deployer.extract.interpolate.glob";
	public static final String PROPERTY_KEY_PREFIX_EXTRACT_ROOT             = "deployer.extract.root";
	public static final String PROPERTY_KEY_PREFIX_EXTRACT_GLOB             = "deployer.extract.glob";
	public static final String PROPERTY_KEY_PREFIX_EXTRACT_SKIP_GLOB        = "deployer.extract.skip.glob";
	public static final String PROPERTY_KEY_LOCAL_REPOSITORY                = "deployer.local.repository";
	public static final String PROPERTY_KEY_REMOTE_REPOSITORY               = "deployer.remote.repository";
	public static final String PROPERTY_KEY_PREFIX_JAVA_ARGS                = "deployer.java.arg";
	public static final String PROPERTY_KEY_RESOLVE_TRANSITIVE              = "deployer.artifact.transitive";
	public static final String PROPERTY_KEY_MAIN_CLASS                      = "deployer.launch.mainclass";
	public static final String PROPERTY_KEY_CLASSPATH                       = "deployer.launch.classpath";
	public static final String PROPERTY_KEY_LAUNCH_JAR                      = "deployer.launch.jar";
	public static final String PROPERTY_KEY_LAUNCH_BINARY                   = "deployer.launch.binary";
	public static final String PROPERTY_KEY_LAUNCH_ARTIFACT                 = "deployer.launch.artifact";
	public static final String PROPERTY_KEY_WDIR                            = "deployer.launch.wdir";
	public static final String PROPERTY_KEY_EXTRA_ENV_KEYS                  = "deployer.launch.env.keys";
	public static final String PROPERTY_KEY_STATISTICS_URI                  = "deployer.statistics.uri";
	public static final String PROPERTY_KEY_STATISTICS_FLUSHINTERVAL        = "deployer.statistics.flushinterval";
}