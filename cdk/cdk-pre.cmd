cd ..\core
call mvn clean install

cd ..\ebl-surrender
call mvn clean install

cd ..\sandbox
call mvn clean install

cd ..\webui
call ng build --configuration=dev

cd ..\cdk
