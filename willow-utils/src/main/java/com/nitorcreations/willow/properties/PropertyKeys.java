package com.nitorcreations.willow.properties;

@SuppressWarnings("PMD.UnusedModifier")
public interface PropertyKeys {
  String PROPERTY_KEY_LAUNCH_URLS = "deployer.launch.urls";
  String PROPERTY_KEY_PREFIX_LAUNCH = "deployer.launch";
  String PROPERTY_KEY_PREFIX_SHUTDOWN = "deployer.shutdown";
  String PROPERTY_KEY_SHUTDOWN_DOWNLOAD = "deployer.shutdown.download";
  String PROPERTY_KEY_DOWNLOAD_DIRECTORY = "deployer.download.directory";
  String PROPERTY_KEY_DOWNLOAD_RETRIES = "deployer.download.retries";
  String PROPERTY_KEY_REMOTE_REPOSITORY = "deployer.remote.repository";
  String PROPERTY_KEY_STATISTICS_URI = "deployer.statistics.uri";
  String PROPERTY_KEY_STATISTICS_FLUSHINTERVAL = "deployer.statistics.flushinterval";
  String PROPERTY_KEY_PREFIX_STATISTICS = "deployer.statistics";
  String PROPERTY_KEY_WORKDIR = "deployer.workdir";
  String PROPERTY_KEY_PROPERTIES_FILENAME = "deployer.properties.filename";
  String PROPERTY_KEY_DEPLOYER_NAME = "deployer.name";
  String PROPERTY_KEY_DEPLOYER_LAUNCH_INDEX = "deployer.launch.index";
  String PROPERTY_KEY_DEPLOYER_HOST = "deployer.host";
  String PROPERTY_KEY_PREFIX_POST_STOP = "deployer.post.stop";
  String PROPERTY_KEY_PREFIX_POST_START = "deployer.post.start";
  String PROPERTY_KEY_PREFIX_PRE_START = "deployer.pre.start";
  String PROPERTY_KEY_PREFIX_POST_STOP_OLD = "deployer.post.stop.old";
  String PROPERTY_KEY_PREFIX_POST_DOWNLOAD = "deployer.post.download";
  String PROPERTY_KEY_PREFIX_PRE_DOWNLOAD = "deployer.pre.download";
  String PROPERTY_KEY_PREFIX_DOWNLOAD = "deployer.download";
  String PROPERTY_KEY_PREFIX_DOWNLOAD_URL = "deployer.download.url";
  String PROPERTY_KEY_PREFIX_DOWNLOAD_ARTIFACT = "deployer.download.artifact";
  String PROPERTY_KEY_SUFFIX_METHOD = "method";
  String PROPERTY_KEY_SUFFIX_ARGS = "arg";
  String PROPERTY_KEY_SUFFIX_BINARY = "binary";
  String PROPERTY_KEY_SUFFIX_ARTIFACT = "artifact";
  String PROPERTY_KEY_SUFFIX_EXTRA_ENV_KEYS = "env.keys";
  String PROPERTY_KEY_SUFFIX_JAVA_ARGS = "java.arg";
  String PROPERTY_KEY_SUFFIX_RESOLVE_TRANSITIVE = "artifact.transitive";
  String PROPERTY_KEY_SUFFIX_MAIN_CLASS = "java.mainclass";
  String PROPERTY_KEY_SUFFIX_CLASSPATH = "java.classpath";
  String PROPERTY_KEY_SUFFIX_JAR = "java.jar";
  String PROPERTY_KEY_SUFFIX_LAUNCH_WORKDIR = "workdir";
  String PROPERTY_KEY_SUFFIX_AUTORESTART = "autorestart";
  String PROPERTY_KEY_SUFFIX_TIMEOUT = "timeout";
  String PROPERTY_KEY_SUFFIX_TERM_TIMEOUT = "term.timeout";
  String PROPERTY_KEY_SUFFIX_DOWNLOAD_FINALPATH = "finalpath";
  String PROPERTY_KEY_SUFFIX_DOWNLOAD_MD5 = "md5";
  String PROPERTY_KEY_SUFFIX_DOWNLOAD_IGNORE_MD5 = "ignore.md5";
  String PROPERTY_KEY_SUFFIX_RETRIES = "retries";
  String PROPERTY_KEY_SUFFIX_EXTRACT_ROOT = "extract.root";
  String PROPERTY_KEY_SUFFIX_EXTRACT_GLOB = "extract.glob";
  String PROPERTY_KEY_SUFFIX_EXTRACT_SKIP_GLOB = "extract.skip.glob";
  String PROPERTY_KEY_SUFFIX_EXTRACT_FILTER_GLOB = "extract.filter.glob";
  String PROPERTY_KEY_SUFFIX_WRITE_MD5SUMS = "extract.writemd5sums";
  String PROPERTY_KEY_SUFFIX_EXTRACT_OVERWRITE = "extract.overwrite";
  String PROPERTY_KEY_SUFFIX_PROXYAUTOCONF = "proxyautoconf";
  String PROPERTY_KEY_SUFFIX_PROXY = "proxy";
  String PROPERTY_KEY_SUFFIX_SKIPOUTPUTREDIRECT = "skipOutputRedirect";
  String ENV_DEPLOYER_TERM_TIMEOUT = "W_DEPLOYER_TERM_TIMEOUT";
  String ENV_DEPLOYER_LOCAL_REPOSITORY = "W_DEPLOYER_LOCAL_REPOSITORY";
  String ENV_DEPLOYER_HOME = "W_DEPLOYER_HOME";
  String ENV_DEPLOYER_NAME = "W_DEPLOYER_NAME";
  String ENV_DEPLOYER_IDENTIFIER = "W_DEPLOYER_IDENTIFIER";
}
