/**
 * DASH Website Demo
 * a Website Demo supporting DASH media content upload, generate and watch.一个支持DASH内容上传、生成、观看的网站示例。
 * dont support live DASH currently
 * 
 * Dependencies:
 * DASHEncoder2 (https://github.com/zhanghuicuc/DASHEncoder2) and FFmpeg 
 *
 * 张晖
 * Hui Zhang
 * 中国传媒大学/数字电视技术
 * Communication University of China/Digital Video Technology
 * 
 * zhanghuicuc@gmail.com
 * http://blog.csdn.net/nonmarking
 * 
 * this website is based on Lei Xiaohua's simplest video website
 */
package util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.struts2.ServletActionContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import bean.Configure;
import bean.Video;
import bean.Videostate;


import service.BaseService;


/**
 * @author 雷霄骅
 * 截取缩略图
 */
public class VideoThumbnailThread extends Thread {
	private ServletContext servletContext;
	
	public ServletContext getServletContext() {
		return servletContext;
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}
	public VideoThumbnailThread(ServletContext servletContext) {
		super();
		this.servletContext = servletContext;
	}
	public void run() {
		try {
		int order=2;
		WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
		BaseService baseService = (BaseService)ctx.getBean("BaseService"); 
		
		Configure thumbnail_ss_cfg=(Configure) baseService.ReadSingle("Configure", "name", "thumbnail_ss");
		Configure folder_thumbnail_cfg=(Configure) baseService.ReadSingle("Configure", "name", "folder_thumbnail");

		String realthumbnailDir=servletContext.getRealPath("/").replace('\\', '/')+folder_thumbnail_cfg.getVal();
		//Check
		File realthumbnailDirFile =new File(realthumbnailDir);
		if(!realthumbnailDirFile.exists()  && !realthumbnailDirFile.isDirectory()){
			System.out.println("Directory not exist. Create it.");  
			System.out.println(realthumbnailDirFile);  
			realthumbnailDirFile.mkdir();
		}
		
		do{
			List<Video> resultvideo=baseService.ReadByProperty("Video","videostate.order", order);
			Videostate nextvideostate=(Videostate) baseService.ReadSingle("Videostate","order", order+1);
			
			Videostate nextvideostate2=(Videostate) baseService.ReadSingle("Videostate","order", order+2);
				if(resultvideo!=null){
					for(Video video:resultvideo){
						String realfileoriPath;
						if(video.getIslive()==0){
							realfileoriPath=servletContext.getRealPath("/").replace('\\', '/')+video.getOriurl();
							//System.out.println(realfileoriPath);
						}else{
							/*realfileoriPath=video.getUrl();
							String a[] = realfileoriPath.split(":");  
							//RTMP FIX: libRTMP URL
						    if(a[0].equals("rtmp")||a[0].equals("rtmpe")||a[0].equals("rtmpte")||a[0].equals("rtmps")){
						    	realfileoriPath=realfileoriPath+" live=1";
						    }*/
							continue;
						}
						String realthumbnailPath=realthumbnailDir+"/"+video.getName()+".jpg";
						
						String videothumbnailcommand="cmd /c start ffmpeg -y -i "+"\""+realfileoriPath+"\""+
						" -ss "+thumbnail_ss_cfg.getVal()+" -s 220x110 -f image2 -vframes 1 "+"\""+realthumbnailPath+"\"";
						System.out.println(videothumbnailcommand);
						Process process=Runtime.getRuntime().exec(videothumbnailcommand);
						//------------------------
						BufferedInputStream in = new BufferedInputStream(process.getInputStream());  
						BufferedReader inBr = new BufferedReader(new InputStreamReader(in));  
						String lineStr;  
						while ((lineStr = inBr.readLine()) != null)  
								System.out.println(lineStr);
						if (process.waitFor() != 0) {  
							if (process.exitValue() == 1)//p.exitValue()==0表示正常结束，1：非正常结束  
								System.err.println("Failed!");  
						}  
						inBr.close();  
						in.close();  
						
						video.setThumbnailurl(folder_thumbnail_cfg.getVal()+"/"+video.getName()+".jpg");
						
						if(video.getIslive()==0){
							video.setVideostate(nextvideostate);
						}else{
							video.setVideostate(nextvideostate2);
						}
						
						baseService.update(video);
						//Rest--------------------------
						sleep(10 * 1000);
					}
				}
			sleep(10 * 1000);
		}while(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
	}

}
