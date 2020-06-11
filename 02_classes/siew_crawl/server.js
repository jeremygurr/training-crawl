const express = require('express');
const app = new express();

//obtain and output the html file to page requests at port 3000.
app.get('/', function(req, res){
  res.sendFile('/home/jaredgurr/training-crawl/02_classes/siew_crawl/index.html');
});
app.listen(3000);

console.log('server is running on port number 3000');
