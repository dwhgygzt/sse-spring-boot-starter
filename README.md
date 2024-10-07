
# 目录
- [基本概念](#基本概念)
- [引入组件](#引入组件)
- [配置组件](#配置组件)
- [事件接口](#事件接口)
- [启动服务](#启动服务)
- [验证服务](#验证服务)

# 基本概念

SSE (Server-Sent Events) 是一种用于在浏览器和 Web 服务器之间建立持久连接的技术。
它允许服务器向客户端发送实时数据，而无需客户端发送任何请求。

- 前端JS原生支持
- 前端JS单向接受服务端消息
- 前端JS支持断线重连
- 简单轻量级text/event-stream 格式

SSE 消息协议格式，以下是完整的消息格式：

```shell
id: 12345  
event: myEvent  
data: Hello, World!  
data: This is a second line.  
retry: 10000
```

各字段是非必填字段，但至少需要包含一个 `data` 字段。

| 字段      | 描述                                                   | 示例                                                      | 必填 |  
|---------|------------------------------------------------------|---------------------------------------------------------|----|  
| `data`  | 消息的主要内容，可以包含多行数据。每行以换行符结束，最后一行后需要一个额外的换行符来表示消息结束。    | `data: Hello, World!\ndata: This is a second line.\n\n` | 是  |  
| `event` | 可选字段，用于指定事件的类型。如果未指定，默认事件类型为 `message`。              | `event: myEvent`                                        | 否  |  
| `id`    | 可选字段，用于标识事件的唯一性。客户端可以使用此 ID 来跟踪事件，尤其是在重新连接时。         | `id: 12345`                                             | 否  |  
| `retry` | 可选字段，用于指定客户端在连接丢失后重新连接的时间（以毫秒为单位）。如果未指定，默认重试时间为 3 秒。 | `retry: 10000`                                          | 否  |  


# 引入组件

SpringBoot工程 pom.xml 中引入依赖：

```xml

<dependencies>
    <dependency>
        <groupId>com.guzt</groupId>
        <artifactId>sse-spring-boot-starter</artifactId>
        <version>最新release版本</version>
    </dependency>
</dependencies>

```

# 配置组件

springboot application.yml 配置如下：

```yaml
guzt:
  sse:
    enable: true
    port: 8849
    event-endpoints:
      - { endpoint: /sse }
      - { endpoint: /events2, retry-milliseconds: 3000 }
      - { endpoint: /events, event-period-milliseconds: 2000, retry-milliseconds: 2000 }
    max-content-length: 1048576
    max-tcp-connections: 100
```

| 配置项                         | 描述                              | 默认值     |  
|-----------------------------|---------------------------------|---------|  
| `enable`                    | 是否启用 SSE 服务，默认为 true            | true    |  
| `port`                      | SSE 服务监听的端口，**可以不配置**           | 8849    |  
| `event-endpoints`           | SSE 服务支持的 endpoint 列表 **可以不配置** | -       |  
| `endpoint`                  | endpoint 名称，必须唯一，**可以不配置**      | /events |  
| `retry-milliseconds`        | 重连时间，单位为毫秒，**可以不配置**            | 3000    |  
| `event-period-milliseconds` | 【重要】`事件发送周期`，单位为毫秒，**可以不配置**    | 5000    |  
| `max-content-length`        | 最大内容长度，单位为字节 **可以不配置**          | 1048576 |  
| `max-tcp-connections`       | 最大 TCP 连接数   **可以不配置**          | 512     |

# 事件接口
针对不同的endpoint，推送不同的事件内容，示例如下：
在自己的SpringBoot服务中，创建一个Service，继承接口 `com.guzt.starter.sse.service.SseBusinessService`

```java

import cn.hutool.core.date.DateUtil;
import com.guzt.starter.sse.pojo.dto.EventDTO;
import com.guzt.starter.sse.service.SseBusinessService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MySseBusinessService implements SseBusinessService {
    @Override
    public boolean connectAuth(String uri, Map<String, String> headers, Map<String, List<String>> parameters) {
        // 根据 uri，headers，parameters 判断是否允许连接
        return true;
    }

    @Override
    public EventDTO generateEvent(String uri, Map<String, String> headers, Map<String, List<String>> parameters) {
        // 根据 uri，headers，parameters 生成 EventDTO
        // 查询缓存或者数据库构建一个 EventDTO
        if (uri.equals("/events2")) {
            return new EventDTO("update", "当前update时间：" + DateUtil.now());
        }
        return new EventDTO("当前时间：" + DateUtil.now());
    }
}
```
上面的 EventDTO 是一个简单的事件对象，包含协议里面的所有字段，默认构造器即可，表示传输message事件，
如果需要自定义事件，可以参考`new EventDTO("update", "当前update时间：" + DateUtil.now());`

- 接口中的参数 uri 是当前请求的 endpoint
- headers 是当前GET请求的 headers 内容，例如 `Accept-Encoding: gzip`
- parameters 是当前GET请求的 parameters 内容， 例如 `/sse?id=123` 中的 id=123
- generateEvent 方法， 返回值 EventDTO 是当前事件的内容，可以包含多个 data，例如：
- connectAuth 方法，用于验证客户端是否允许连接，返回 true 表示允许连接，返回 false 表示不允许连接。


# 启动服务

控制台日志信息如下：

```shell
DEBUG 3192 -- [ main] c.guzt.starter.sse.service.SseServer   : SSE服务初始化完毕
DEBUG 3192 -- [MyTaskExecutorThreadPool_1] c.guzt.starter.sse.service.SseServer   : SSE服务正在启动...
DEBUG 3192 -- [MyTaskExecutorThreadPool_1] c.guzt.starter.sse.service.SseServer   : SSE服务启动完成，绑定端口：8849
```

表示SSE服务启动成功

# 验证服务
给定一个前端页面代码如下：

可以指定一个 endpoint，例如：`http://localhost:8849/sse`

可以指定一个事件名称，例如：`update`，默认为 `message`

可以开启/关闭连接，默认需要手动点击开启/关闭连接

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SSE Example</title>
    <script>
        let eventSource; // 声明 eventSource 变量
        let isConnected = false; // 连接状态标志

        function toggleEventSource() {
            const button = document.getElementById('toggleButton');
            const inputUrl = document.getElementById('urlInput').value.trim(); // 获取输入框中的 URL
            const inputEvent = document.getElementById('eventInput').value.trim(); // 获取事件名称输入框的内容

            if (!inputUrl) {
                alert('请输入有效的 URL');
                return;
            }

            if (isConnected) {
                eventSource.close(); // 关闭连接
                button.innerText = "开启 EventSource"; // 更新按钮文本
                button.classList.remove('close'); // 移除关闭状态的样式
                button.classList.add('open'); // 添加开启状态的样式
                document.getElementById('status').innerText = "Disconnected from SSE server.";
                document.getElementById('status').style.color = "red";
                isConnected = false; // 更新连接状态
            } else {
                eventSource = new EventSource(inputUrl); // 使用输入框中的 URL 创建新的 EventSource 实例

                eventSource.onopen = function() {
                    console.log("Connection to server opened.");
                    const status = document.getElementById('status');
                    status.innerText = "Connected to SSE server.";
                    status.style.color = "green";
                    status.style.fontWeight = "bold";
                    button.innerText = "关闭 EventSource"; // 更新按钮文本
                    button.classList.remove('open'); // 移除开启状态的样式
                    button.classList.add('close'); // 添加关闭状态的样式
                    isConnected = true; // 更新连接状态
                };

                // 处理 message 类型的事件
                eventSource.onmessage = function(event) {
                    console.log("Received message: " + event.data);
                    displayMessage(event.data);
                };

                // 动态处理自定义事件类型
                if (inputEvent) {
                    eventSource.addEventListener(inputEvent, function(event) {
                        console.log("Received event (" + inputEvent + "): " + event.data);
                        displayMessage(`Event (${inputEvent}): ${event.data}`);
                    });
                }

                eventSource.onerror = function() {
                    console.error("EventSource failed.");
                    const status = document.getElementById('status');
                    status.innerText = "Exception: Connection to SSE server lost.";
                    status.style.color = "red";
                    status.style.fontWeight = "bold";
                    button.innerText = "开启 EventSource"; // 更新按钮文本
                    button.classList.remove('close'); // 移除关闭状态的样式
                    button.classList.add('open'); // 添加开启状态的样式
                    isConnected = false; // 更新连接状态
                };
            }
        }

        function displayMessage(message) {
            const messagesDiv = document.getElementById('messages');
            messagesDiv.innerHTML += `<p>${message}</p>`;

            // 检查行数并在超过100行时清空内容
            const lines = messagesDiv.getElementsByTagName('p').length;
            if (lines > 100) {
                messagesDiv.innerHTML = ''; // 清空内容
                console.log("Messages cleared after exceeding 100 lines.");
            }
        }
    </script>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
            padding: 20px;
            background-color: #f4f4f4;
            border-radius: 8px;
        }
        #toggleButton {
            padding: 10px 20px; /* 增加内边距 */
            font-size: 16px; /* 增大字体 */
            color: white; /* 字体颜色 */
            border: none; /* 去掉边框 */
            border-radius: 5px; /* 圆角 */
            cursor: pointer; /* 鼠标悬停时显示手型 */
            transition: background-color 0.3s; /* 背景颜色过渡效果 */
        }
        #toggleButton.open {
            background-color: #007bff; /* 开启状态按钮背景颜色 */
        }
        #toggleButton.open:hover {
            background-color: #0056b3; /* 开启状态悬停时的背景颜色 */
        }
        #toggleButton.open:active {
            background-color: #004080; /* 开启状态点击时的背景颜色 */
        }
        #toggleButton.close {
            background-color: #f17b87; /* 关闭状态按钮背景颜色 */
        }
        #toggleButton.close:hover {
            background-color: #d9534f; /* 关闭状态悬停时的背景颜色 */
        }
        #toggleButton.close:active {
            background-color: #c9302c; /* 关闭状态点击时的背景颜色 */
        }
        #urlInput, #eventInput {
            width: 400px; /* 输入框宽度 */
            padding: 8px; /* 增加内边距 */
            font-size: 16px; /* 增大字体 */
            margin-right: 10px; /* 添加与按钮的间距 */
        }
        #messages {
            margin-top: 20px;
            padding: 10px;
            background-color: #fff;
            border: 1px solid #ccc;
            border-radius: 4px;
            max-height: 300px;
            overflow-y: auto;
        }
        p {
            margin: 5px 0;
            font-size: 18px; /* 增大内容字体大小 */
        }
        #status {
            font-size: 20px; /* 增大状态字体大小 */
        }
    </style>
</head>
<body>
<h1>SSE Example</h1>
<label for="urlInput"></label><input type="text" id="urlInput" placeholder="Enter SSE URL" value="http://localhost:8849/events"/> <!-- URL 输入框 -->
<label for="eventInput"></label><input type="text" id="eventInput" placeholder="Enter event type (e.g., myEvent)" /> <!-- 事件输入框 -->
<button id="toggleButton" class="open" onclick="toggleEventSource()">开启 EventSource</button>
<br/><br/><br/>
<div id="status">请点击 [开启 EventSource] 按钮开启 EventSource。</div>
<br/>
<div id="messages"></div>
</body>
</html>

```