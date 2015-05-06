System.config({
  "baseURL": "/",
  "transpiler": "babel",
  "babelOptions": {
    "optional": [
      "runtime"
    ]
  },
  "paths": {
    "*": "*.js",
    "github:*": "target/jspm_packages/github/*.js",
    "npm:*": "target/jspm_packages/npm/*.js"
  }
});

System.config({
  "map": {
    "babel": "npm:babel-core@5.1.13",
    "babel-runtime": "npm:babel-runtime@5.1.13",
    "chjj/term.js": "github:chjj/term.js@0.0.4",
    "core-js": "npm:core-js@0.9.4",
    "cubism": "npm:cubism@1.6.0",
    "d3": "npm:d3@3.5.5",
    "intercom.js": "github:diy/intercom.js@0.1.4",
    "nvd3": "npm:nvd3@1.1.15",
    "t3js": "npm:t3js@1.2.0",
    "github:jspm/nodelibs-process@0.1.1": {
      "process": "npm:process@0.10.1"
    },
    "github:jspm/nodelibs-util@0.1.0": {
      "util": "npm:util@0.10.3"
    },
    "npm:core-js@0.9.4": {
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:cubism@1.6.0": {
      "d3": "npm:d3@3.5.5",
      "util": "github:jspm/nodelibs-util@0.1.0"
    },
    "npm:inherits@2.0.1": {
      "util": "github:jspm/nodelibs-util@0.1.0"
    },
    "npm:nvd3@1.1.15": {
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:util@0.10.3": {
      "inherits": "npm:inherits@2.0.1",
      "process": "github:jspm/nodelibs-process@0.1.1"
    }
  }
});

