#!/bin/bash
cd "${0%/*}"
mvn clean package
read -n1 -r -p 'Press any key to continue . . . ' key
