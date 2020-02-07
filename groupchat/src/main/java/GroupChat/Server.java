package GroupChat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

public class Server {
    private Selector selector;
    private ServerSocketChannel listenChannel;
    private static final int PORT = 6667;

    public Server(){
        try {
            //得到选择器
            selector=Selector.open();
            //ServerSocketChannel
            listenChannel = ServerSocketChannel.open();
            //绑定端口
            listenChannel.socket().bind(new InetSocketAddress(PORT));
            //设置非阻塞模式
            listenChannel.configureBlocking(false);
            //将该listenChannel 注册到selector
            listenChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //监听
    public void listen(){
        try {
            while (true){
                int count = selector.select();
                //有事情处理
                if(count > 0 ){
                    //遍历得到selectionkey 集合
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while(iterator.hasNext()){
                        //取出selectionkey
                        SelectionKey key = iterator.next();

                        //监听到accept
                        if(key.isAcceptable()){
                            SocketChannel sc = listenChannel.accept();
                            sc.configureBlocking(false);
                            //将sc注册到selector
                            sc.register(selector,SelectionKey.OP_READ);
                            //提示
                            System.out.println(sc.getRemoteAddress()+"上线啦！");
                        }
                        //通道发送read事件，通道是可读的状态
                        if(key.isReadable()){
                            //处理读，专门写方法
                            readData(key);
                        }
                        //删除当前的key 防止重复处理
                        iterator.remove();
                    }
                }else {
                    System.out.println("等待");
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //读取客户端消息
    private void readData(SelectionKey key){
        //定义一个socketchannel
        SocketChannel channel = null;
        try{
            channel=(SocketChannel)key.channel();
            //创建缓冲buffer
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int count = channel.read(buffer);

            //根据count的信息处理
            if(count>0){
                //把缓存区的数据转换成字符串
                String msg=new String(buffer.array());
                //输出消息
                System.out.println("from 客户端 "+msg);
                //向其他用户转发消息，去掉自己，专门一个方法
                sengInfoToOtherClients(msg,channel);
            }
        }catch (IOException e){
            try {
                System.out.println(channel.getRemoteAddress()+"离线了");
                //取消注册
                key.cancel();
                //关闭通道
                channel.close();
            }catch (IOException ee){
                ee.printStackTrace();
            }

        }
    }

    //转发消息给其他客户端
    private void sengInfoToOtherClients(String msg,SocketChannel self) throws IOException {
        System.out.println("服务器转发消息给其他客户端中。。。");
        //遍历注册到selector上的socketchannel 排除自己
        for(SelectionKey key : selector.keys()){
            //通过key获得对应的socketchannel
            Channel targetChannel = key.channel();
            if(targetChannel instanceof SocketChannel &&targetChannel != self){
                //转型
                SocketChannel dest = (SocketChannel) targetChannel;
                //把msg存储到buffer
                ByteBuffer byteBuffer = ByteBuffer.wrap(msg.getBytes());
                //将buffer的数据写入通道
                dest.write(byteBuffer);

            }
        }
    }

    public static void main(String[] args) {
        //创建服务器对象
        Server server = new Server();
        server.listen();

    }
}
