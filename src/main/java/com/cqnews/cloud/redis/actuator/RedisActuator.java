package com.cqnews.cloud.redis.actuator;

import com.cqnews.cloud.redis.commands.CommandParse;
import com.cqnews.cloud.redis.commands.ResponseCommand;
import com.cqnews.cloud.redis.datastruct.DataTypeEnum;
import com.cqnews.cloud.redis.db.DB;
import com.cqnews.cloud.redis.helper.Command;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * 执行器
 */
public class RedisActuator implements Actuator{

    // db层
    private DB db;
    // 存档任务指令
    private Queue queue;

    public RedisActuator() {
        // init db
        db = new DB();
    }


    @Override
    public byte[] exec(List<String> commands) {
        String s = commands.get(0);
        Command command = Command.parseCommand(s);
        if (command == null) {
            return ResponseCommand.responseOk();
        }

        // strings
        if (command.getCommand().equals(Command.SET.getCommand())) {
            if (commands.size() >= 3) {
                System.out.println("commands:=" + commands);
                db.doPutString(commands.get(1), commands.get(2).getBytes(), DataTypeEnum.REDIS_STRING);
            }
            return ResponseCommand.responseOk();
        } else if (command.getCommand().equals(Command.GET.getCommand())) {
            if (commands.size() >= 2) {
                System.out.println("commands:=" + commands);
                byte[] value = db.get(commands.get(1), command);
                if (value != null) { // 假设db.get在找不到key时返回null
                    // Redis协议中字符串的响应格式是：$<length>\r\n<data>\r\n
                    int length = value.length;
                    byte[] lengthBytes = String.valueOf(length).getBytes(StandardCharsets.UTF_8);
                    byte[] response = new byte[1+lengthBytes.length+2+length + 2 ]; // $、\r\n、<data>和\r\n的长度
                    System.arraycopy("$".getBytes(StandardCharsets.UTF_8), 0, response, 0, 1);
                    System.arraycopy(lengthBytes, 0, response, 1, lengthBytes.length);
                    System.arraycopy(("\r\n").getBytes(StandardCharsets.UTF_8), 0, response, 1 + lengthBytes.length, 2);
                    System.arraycopy(value, 0, response, 1 + lengthBytes.length + 2, value.length);
                    System.arraycopy(("\r\n").getBytes(StandardCharsets.UTF_8), 0, response, 1 + lengthBytes.length + 2 + value.length, 2);
                    return response;
                } else {
                    return ResponseCommand.responseErr(); // 如果key不存在，返回特殊的字节数组
                }
            }
        } else if (command.getCommand().equals(Command.MSET.getCommand())) {
            System.out.println("commands:=" + commands);
                //mset k1 v1 k2 v2
            if (commands.size() >= 3) {
                for (int i = 1; i < commands.size(); i += 2) {
                    String key = commands.get(i);
                    String value = commands.get(i + 1);
                    db.doPutString(key, value.getBytes(), DataTypeEnum.REDIS_STRING);
                }
            }
        } else if (command.getCommand().equals(Command.MGET.getCommand())) {
            System.out.println("commands:=" + commands);
            List<String> values = new ArrayList<>();
            for (int i = 1; i < commands.size(); i++) {
                String key = commands.get(i);
                byte[] valueBytes = db.get(key,command);
                if (valueBytes != null) {
                    values.add(new String(valueBytes));
                } else {
                    values.add(null);
                }
            }
            return ResponseCommand.responseMGet(values);
        }

        // list
        if (command.getCommand().equals(Command.LPUSH.getCommand())) {
            String key = commands.get(1);
        }




        // ping pong
        if (command.getCommand().equals(Command.PING.getCommand())) {
            return ResponseCommand.responsePing();
        }
        return ResponseCommand.responseOk();
    }
}
