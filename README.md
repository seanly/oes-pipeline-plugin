# asl plugin

一个在Jenkins运行ant-asl的插件，在Jenkins master启动过程中配置-Dasl.root=/path/to/ant-asl

# 调试方法

```bash

cd /opt/
git clone https://github.com/seanly/ant-asl.git

mvn hpi:run -Dasl.root=/opt/ant-asl
```