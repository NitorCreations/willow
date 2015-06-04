#!/bin/bash

phantomjs src/test/resources/parse.js
dot -T png t3model.dot > t3model.png
