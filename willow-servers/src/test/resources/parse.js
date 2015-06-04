String.prototype.endsWith = function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
};
var boxName;
var walk = function(dir, filelist) {
  files = fs.list(dir);
  filelist = filelist || [];
  files.forEach(function(file) {
    if (file == "." || file == "..") return;
    if (fs.isDirectory(dir + file)) {
      filelist = walk(dir + file + '/', filelist);
    } else {
      if (file.endsWith(".js")) {
        filelist.push(dir + file);
      }
    }
  });
  return filelist;
};

var esprima = require('esprima');
var escodegen = require('escodegen');
var fs = require('fs');
var files = walk('src/main/resources/webapp/scripts/modules/',
  walk('src/main/resources/webapp/scripts/services/',
  walk('src/main/resources/webapp/scripts/behaviors/')));

var modules = [];
var services = [];
var behaviors = [];
var messages = [];
var globals = [];
for (var x=0; x<files.length;x++) {
  var script = fs.read(files[x]);
  var body = esprima.parse(script).body;
  for (var i=0; i<body.length;i++) {
    var obj = parseModule(body[i]);
    if (body[i].expression.callee.property.name == "addModule") {
      modules.push(obj);
    } else if (body[i].expression.callee.property.name == "addService") {
      services.push(obj);
    } else if (body[i].expression.callee.property.name == "addBehavior") {
      behaviors.push(obj);
    }
    if (obj.onmessage) {
      for (message in obj.onmessage) {
        if (messages.indexOf(message) == -1) {
          messages.push(message);
        }
      }
    }
    if (obj.broadcasts) {
      for (func in obj.broadcasts) {
        if (obj.broadcasts[func].message &&
          messages.indexOf(obj.broadcasts[func].message) == -1) {
            messages.push(obj.broadcasts[func].message);
          }
      }
    }
    if (obj.globals) {
      for (var y=0;y<obj.globals.length;y++) {
        if (globals.indexOf(obj.globals[y]) == -1) {
          globals.push(obj.globals[y]);
        }
      }
    }
  }
}
console.log("========================================")
console.log("=============== GLOBALS ================")
console.log("========================================")
console.log(JSON.stringify(globals, null, 2));
console.log("========================================")
console.log("============== MESSAGES ================")
console.log("========================================")
console.log(JSON.stringify(messages, null, 2));
console.log("========================================")
console.log("============== SERVICES ================")
console.log("========================================")
console.log(JSON.stringify(services, null, 2));
console.log("========================================")
console.log("============== BEHAVIORS ===============")
console.log("========================================")
console.log(JSON.stringify(behaviors, null, 2));
console.log("========================================")
console.log("=============== MODULES ================")
console.log("========================================")
console.log(JSON.stringify(modules, null, 2));
var out = "t3model.dot";
fs.write(out, "digraph G {\n", "w");

writeSimpleNodes(globals);
writeSimpleNodes(messages);
writeModuleNodes(services);
writeModuleNodes(behaviors);
writeModuleNodes(modules);
writeModuleDeps(services);
writeModuleDeps(behaviors);
writeModuleDeps(modules);

fs.write(out, "}\n", "a");

phantom.exit();

function writeSimpleNodes(arr) {
  for (var i=0; i<arr.length;i++) {
    fs.write(out, arr[i].replace(/-/g, "_") + " [\n  label = \"{" + arr[i] + "}\"\n  shape = \"record\"\n]\n", "a");
  }
}
function writeModuleNodes(arr) {
  for (next in arr) {
    var mod = arr[next];
    fs.write(out, mod.name.replace(/-/g, "_") + " [\n  label = \"{" + moduleLabel(mod) + "}\"\n  shape = \"record\"\n]\n", "a");
  }
}
function writeModuleDeps(arr) {
  for (next in arr) {
    var mod = arr[next];
    for (var i=0;i<mod.globals.length;i++) {
      fs.write(out, "edge [\n  arrowhead = \"normal\"\n  headlabel = \"\"\n", "a");
      fs.write(out, "  taillabel = \"\"\n", "a")
      fs.write(out, "]\n", "a");
      fs.write(out, mod.name.replace(/-/g, "_") + " -> "
        + mod.globals[i].replace(/-/g, "_") + "\n", "a");
    }
    for (var i=0;i<mod.services.length;i++) {
      fs.write(out, "edge [\n  arrowhead = \"normal\"\n  headlabel = \"\"\n", "a");
      fs.write(out, "  taillabel = \"\"\n", "a")
      fs.write(out, "]\n", "a");
      fs.write(out, mod.name.replace(/-/g, "_") + " -> "
        + mod.services[i].replace(/-/g, "_") + "\n", "a");
    }
    if (mod.behaviors) {
      for (var i=0;i<mod.behaviors.length;i++) {
        fs.write(out, "edge [\n  arrowhead = \"normal\"\n  headlabel = \"\"\n", "a");
        fs.write(out, "  taillabel = \"\"\n", "a")
        fs.write(out, "]\n", "a");
        fs.write(out, mod.name.replace(/-/g, "_") + " -> "
          + mod.behaviors[i].replace(/-/g, "_") + "\n", "a");
      }
    }
    if (mod.broadcasts) {
      for (func in mod.broadcasts) {
        for (var i=0;i<mod.broadcasts[func].length;i++) {
          var msg = mod.broadcasts[func][i];
          fs.write(out, "edge [\n  arrowhead = \"normal\"\n  taillabel = \"" + func
            + "\"\n", "a");
          if (msg.data) {
            fs.write(out, "  headlabel = \"" + msg.data + "\"\n", "a")
          } else {
            fs.write(out, "  headlabel = \"\"\n", "a")
          }
          fs.write(out, "]\n", "a");
          fs.write(out, mod.name.replace(/-/g, "_") + " -> "
            + msg.message.replace(/-/g, "_") + "\n", "a");
        }
      }
    }
    if (mod.onmessage) {
      for (rmsg in mod.onmessage) {
        fs.write(out, "edge [\n  arrowhead = \"normal\"\n  headlabel = \"" +
          mod.onmessage[rmsg] + "\"\n", "a");
        fs.write(out, "  taillabel = \"\"\n", "a")
        fs.write(out, "]\n", "a");
        fs.write(out, rmsg.replace(/-/g, "_") + " -> "
          + mod.name.replace(/-/g, "_") + "\n", "a");
      }
    }
  }
}
function moduleLabel(mod) {
  var ret = mod.name;
  if (mod.public) {
    ret = ret + "|";
    for (var i=0;i<mod.public.length;i++) {
      ret = ret + mod.public[i] + "\\l";
    }
  }
  return ret;
}
function parseModule(bodyItem) {
  var ret = {};
  ret.name = bodyItem.expression.arguments[0].value;
  var moduleFunction = bodyItem.expression.arguments[1];
  boxName = moduleFunction.params[0].name;
  var moduleBody = moduleFunction.body.body;
  var messages;
  ret.globals = findGlobals(moduleFunction);
  ret.services = findServices(moduleFunction);
  for (var j=0;j<moduleBody.length;j++) {
    if (moduleBody[j].type == "FunctionDeclaration") {
      var functionName = moduleBody[j].id.name;
      var broadcast = findBroadcast(moduleBody[j]);
      if (broadcast.length > 0) {
        ret.broadcasts = ret.broadcasts || {};
        ret.broadcasts[functionName] = ret.broadcasts[nextName] || [];
        for (var x=0;x<broadcast.length;x++) {
          ret.broadcasts[functionName].push(broadcast[x]);
        }
      }
    } else if (moduleBody[j].type == "ReturnStatement") {
      var returnBody = moduleBody[j].argument.properties;
      if (returnBody) {
        for (var k=0;k<returnBody.length;k++) {
          if (returnBody[k].key.name == "messages") {
            messages = eval(escodegen.generate(returnBody[k].value));
          } else if (returnBody[k].key.name == "behaviors") {
            ret.behaviors = eval(escodegen.generate(returnBody[k].value));
          } else {
            ret.public = ret.public || [];
            var functionName = returnBody[k].key.name;
            var fArgs = "";
            if (returnBody[k].value.params &&
              returnBody[k].value.params.length > 0) {
              for (arg in returnBody[k].value.params) {
                fArgs = fArgs + ", " + escodegen.generate(returnBody[k].value.params[arg]);
              }
            }
            if (fArgs.length > 0) {
              fArgs = fArgs.substring(2);
            }
            ret.public.push(functionName + "(" + fArgs + ")");
            var broadcast = findBroadcast(returnBody[k]);
            if (broadcast.length > 0) {
              ret.broadcasts = ret.broadcasts || {};
              ret.broadcasts[functionName] = ret.broadcasts[nextName] || [];
              for (var x=0;x<broadcast.length;x++) {
                ret.broadcasts[functionName].push(broadcast[x]);
              }
            }
          }
        }
      } else {
        console.log("============================================");
        console.log("=========== " + escodegen.generate(moduleBody[j]));
        console.log("============================================");
      }
    } else if (moduleBody[j].type == "VariableDeclaration") {
      var decls = moduleBody[j].declarations;
      for (var k=0;k<decls.length; k++) {
        var nextVar = decls[k];
        var varName = nextVar.id.name;
        if (nextVar.init && nextVar.init.type == "FunctionExpression") {
          var broadcast = findBroadcast(nextVar.init);
          if (broadcast.length > 0) {
            ret.broadcasts = ret.broadcasts || {};
            ret.broadcasts[varName] = ret.broadcasts[nextName] || [];
            for (var x=0;x<broadcast.length;x++) {
              ret.broadcasts[varName].push(broadcast[x]);
            }
          }
        } else if (nextVar.init && nextVar.init.type == "ObjectExpression") {
          var props = nextVar.init.properties;
          for (var l=0;l<props.length;l++) {
            var nextProp = props[l];
            var nextName = varName + "." + nextProp.key.name;
            if (nextProp.value && nextProp.value.type == "FunctionExpression") {
              var broadcast = findBroadcast(nextProp.value);
              if (broadcast.length > 0) {
                ret.broadcasts = ret.broadcasts || {};
                ret.broadcasts[nextName] = ret.broadcasts[nextName] || [];
                for (var x=0;x<broadcast.length;x++) {
                  ret.broadcasts[nextName].push(broadcast[x]);
                }
              }
            }
          }
        }
      }
    }
  }
  if (messages) {
    for (var j=0;j<messages.length;j++) {
      traverse(moduleFunction, findMessageSwitch(messages[j], function(getscalled) {
        if (!ret.onmessage) {
          ret.onmessage = {};
        }
        ret.onmessage[messages[j]] = getscalled;
      }));
    }
  }
  return ret;
}
function traverse(node, func) {
  func(node);
  for (var key in node) {
    if (node.hasOwnProperty(key)) {
      var child = node[key];
        if (typeof child === 'object' && child !== null) {
          if (Array.isArray(child)) {
            child.forEach(function(node) {
            traverse(node, func);
          });
        } else {
          traverse(child, func);
        }
      }
    }
  }
}
function findBroadcast(functionNode) {
  var broadcasts = [];
  traverse(functionNode, findBoxFunc(boxName, "broadcast", function(arguments) {
    broadcasts.push({
      message: arguments[0].value,
      data: arguments[1] ? escodegen.generate(arguments[1]) : null
    });
  }));
  return broadcasts;
}
function findGlobals(functionNode) {
  var globals = [];
  traverse(functionNode, findBoxFunc(boxName, "getGlobal", function(arguments) {
    globals.push(arguments[0].value);
  }));
  return globals;
}
function findServices(functionNode) {
  var services = [];
  traverse(functionNode, findBoxFunc(boxName, "getService", function(arguments) {
    services.push(arguments[0].value);
  }));
  return services;
}
function findBoxFunc(boxName, funcName, callback) {
  return function(nextNode) {
    if (nextNode &&
      nextNode.callee &&
      nextNode.callee.object &&
      nextNode.callee.object.name &&
      nextNode.callee.object.name == boxName &&
      nextNode.callee.property &&
      nextNode.callee.property.name &&
      nextNode.callee.property.name == funcName) {
        callback(nextNode.arguments);
    }
  };
}

function findMessageSwitch(messageName, callback) {
  return function(nextNode) {
    if (nextNode &&
      nextNode.type &&
      nextNode.type == "SwitchCase" &&
      nextNode.test &&
      nextNode.test.value &&
      nextNode.test.value == messageName) {
        traverse(nextNode.consequent, function(iterated) {
          if (iterated.type &&
          iterated.type == "CallExpression") {
            callback(escodegen.generate(iterated));
          }
        });
    }
  };
}
