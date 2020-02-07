package GroupChat;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;

public class Client {
    //服务器ip
    private final String HOST="127.0.0.1";
    //服务器端口
    private final int PORT=6667;
    private Selector selector;
    private SocketChannel socketChannel;
    private String username;

    public Client() throws IOException {
        selector = Selector.open();
        //连接服务器
        socketChannel=socketChannel.open(new InetSocketAddress("127.0.0.1",PORT));
        //设置非阻塞
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        username=socketChannel.getRemoteAddress().toString().substring(1);
        System.out.println(username+" is OK...");

    }

    //向服务器发送消息
    public void sendInfo(String info){

        info=username+" 说 "+info;
        try {
            socketChannel.write(ByteBuffer.wrap(info.getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //读取从服务器回复的消息
    public void readInfo(){
        try {
            int readChannels = selector.select();
            //有可用的通道
            if(readChannels>0){
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    if(key.isReadable()){
                        //得到相关通道
                        SocketChannel sc = (SocketChannel)key.channel();
                        //得到一个buffer
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        //从通道读取数据到buffer中
                        sc.read(buffer);
                        //把读到的缓冲区数据转成字符串
                        String msg = new String(buffer.array());
                        System.out.println(msg.trim());
                    }
                }

            }else {
                System.out.println("没有可用的通道");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        //启动一个客户端
        Client chatClint=new Client();
        //启动一个线程 每隔三秒读取从服务器发送的数据
        new Thread(){
            public void run(){
                while (true){
                    chatClint.readInfo();
                    try {
                        Thread.currentThread().sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
        //发送数据给服务端
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()){
            String s =scanner.nextLine();
            chatClint.sendInfo(s);
        }
    }
}
