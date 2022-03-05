#!/bin/bash
cd "${0%/*}"
./mvnw clean package
read -n1 -r -p 'Press any key to continue . . . ' key
