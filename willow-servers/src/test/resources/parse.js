var esprima = require('esprima');
var escodegen = require('escodegen');
var fs = require('fs');
var script = fs.readFileSync('src/main/resources/webapp/scripts/modules/index-horizon-graphs.js');
var body = esprima.parse(script).body;
var boxName;
for (var i=0; i<body.length;i++) {
  if (body[i].expression.callee.property.name == "addModule") {
    console.log(JSON.stringify(parseModule(body[i]), null, 2));
  }
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
        if (!ret.broadcasts) {
          ret.broadcasts = {};
        }
        ret.broadcasts[functionName] = broadcast;
      }
    } else if (moduleBody[j].type == "ReturnStatement") {
      var returnBody = moduleBody[j].argument.properties;
      for (var k=0;k<returnBody.length;k++) {
        if (returnBody[k].key.name == "messages") {
          messages = eval(escodegen.generate(returnBody[k].value));
        } else {
          var functionName = returnBody[k].key.name;
          var broadcast = findBroadcast(returnBody[k]);
          if (broadcast.length > 0) {
            if (!ret.broadcasts) {
              ret.broadcasts = {};
            }
            ret.broadcasts[functionName] = broadcast;
          }
        }
      }
    }
  }
  for (var j=0;j<messages.length;j++) {
    traverse(moduleFunction, findMessageSwitch(messages[j], function(getscalled) {
      if (!ret.onmessage) {
        ret.onmessage = {};
      }
      ret.onmessage[messages[j]] = getscalled;
    }));
  }
  return ret;s
}
function traverse(node, func) {

    func(node);//1
    for (var key in node) { //2
        if (node.hasOwnProperty(key)) { //3
            var child = node[key];
            if (typeof child === 'object' && child !== null) { //4

                if (Array.isArray(child)) {
                    child.forEach(function(node) { //5
                        traverse(node, func);
                    });
                } else {
                    traverse(child, func); //6
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
