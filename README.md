# asl plugin

一个在Jenkins运行asl-steps的插件，在Jenkins master启动过程中配置-Dasl.root=/path/to/asl-steps

# 调试方法

```bash

cd /opt/
git clone https://github.com/seanly/asl-steps.git

mvn hpi:run -Dasl.root=/opt/asl-steps
```
