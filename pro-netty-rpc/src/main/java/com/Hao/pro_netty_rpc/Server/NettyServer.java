package com.Hao.pro_netty_rpc.Server;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;

import com.Hao.pro_netty_rpc.constant.Constants;
import com.Hao.pro_netty_rpc.factory.ZookeeperFactory;
import com.Hao.pro_netty_rpc.handler.ServerHandler;
import com.Hao.pro_netty_rpc.handler.SimpleServerHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;

public class NettyServer {
	public static void main(String[] args) {
		EventLoopGroup parentGruop = new NioEventLoopGroup();
		EventLoopGroup childGruop = new NioEventLoopGroup();
		try {
			ServerBootstrap bootstrap= new ServerBootstrap();		
			bootstrap.group(parentGruop, childGruop);
			bootstrap.option(ChannelOption.SO_BACKLOG, 128)
					.childOption(ChannelOption.SO_KEEPALIVE, false)
					.channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<Channel>() {

						@Override
						protected void initChannel(Channel ch) throws Exception {
							ch.pipeline().addLast(new DelimiterBasedFrameDecoder(65535, Delimiters.lineDelimiter()[0]));
							ch.pipeline().addLast(new StringDecoder());
							ch.pipeline().addLast(new IdleStateHandler(60, 45, 20, TimeUnit.SECONDS));
							ch.pipeline().addLast(new ServerHandler());
							ch.pipeline().addLast(new StringEncoder());
						}
					});
			
			int port = 8084;
			ChannelFuture f= bootstrap.bind(port).sync();
			
			//netty服务器注册到zookeeper上,获取client单例并创建临时节点(断开删除)
			CuratorFramework client = ZookeeperFactory.getClient();
			InetAddress netAddress = InetAddress.getLocalHost();
			client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(Constants.SERVER_PATH+netAddress.getHostAddress()+"#"+port+"#");
			
			f.channel().closeFuture().sync();
		} catch (Exception e) {
			e.printStackTrace();
			parentGruop.shutdownGracefully();
			childGruop.shutdownGracefully();
		}
		
	}
	
}
