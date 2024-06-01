cd ..\core
call mvn clean install

cd ..\booking
call mvn clean install

cd ..\ebl
call mvn clean install

cd ..\ebl-issuance
call mvn clean install

cd ..\ebl-surrender
call mvn clean install

cd ..\jit
call mvn clean install

cd ..\ovs
call mvn clean install

cd ..\pint
call mvn clean install

cd ..\tnt
call mvn clean install

cd ..\sandbox
call mvn clean install

cd ..\lambda
call mvn clean install

cd ..\webui
call npm install
call ng build --configuration=%1%

cd ..\cdk
