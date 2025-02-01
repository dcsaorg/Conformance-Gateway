cd ..
call mvn clean package

cd webui
call npm install
call npm run build -- --configuration=%1%

cd ..\cdk
