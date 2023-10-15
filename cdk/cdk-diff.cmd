call cdk-pre.cmd %1%
cdk -v diff %1%ConformanceStack
