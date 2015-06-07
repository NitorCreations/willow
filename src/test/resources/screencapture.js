var page = require('webpage').create();
page.viewportSize = { width: 1024, height: 768 };
page.settings.userName = "admin";
page.settings.password = "admin";
page.open('http://localhost:5120/', function() {
  window.setTimeout(function () {
    page.render("test.png");
    phantom.exit();
    }, 30000);
});
