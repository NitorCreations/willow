var esprima = require('esprima');
var escodegen = require('escodegen');
var fs = require('fs');
var script = fs.readFileSync('src/main/resources/webapp/scripts/modules/index-horizon-graphs.js');
var body = esprima.parse(script).body;
var boxName;
for (var i=0; i<body.length;i++) {
  if (body[i].expression.callee.property.name == "addModule") {
    var modulename = body[i].expression.arguments[0].value;
    console.log("MODULE: " + modulename);
    var moduleFunction = body[i].expression.arguments[1];
    boxName = moduleFunction.params[0].name;
    var moduleBody = moduleFunction.body.body;
    for (var j=0;j<moduleBody.length;j++) {
      if (moduleBody[j].type == "FunctionDeclaration") {
        console.log("private " + moduleBody[j].id.name);
        console.log("broadcast: " +
          JSON.stringify(findBroadcast(moduleBody[j])));
      } else if (moduleBody[j].type == "ReturnStatement") {
        var returnBody = moduleBody[j].argument.properties;
        for (var k=0;k<returnBody.length;k++) {
          if (returnBody[k].key.name == "messages") {
            console.log("messages: " + escodegen.generate(returnBody[k].value));
          } else {
            console.log("public " + returnBody[k].key.name);
            console.log("broadcast: " +
              JSON.stringify(findBroadcast(returnBody[k])));
          }
        }
      }
    }
  }
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
  traverse(functionNode, function(nextNode) {
    if (nextNode.expression &&
      nextNode.expression.callee &&
      nextNode.expression.callee.object &&
      nextNode.expression.callee.object.name &&
      nextNode.expression.callee.object.name == boxName &&
      nextNode.expression.callee.property &&
      nextNode.expression.callee.property.name &&
      nextNode.expression.callee.property.name == "broadcast")
        broadcasts.push({
          message: nextNode.expression.arguments[0].value,
          data: nextNode.expression.arguments[1] ? escodegen.generate(nextNode.expression.arguments[1]) : null
        });
  });
  return broadcasts;
}
