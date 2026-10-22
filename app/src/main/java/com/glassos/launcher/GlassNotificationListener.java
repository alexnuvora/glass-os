package com.glassos.launcher;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.*;
public class GlassNotificationListener extends NotificationListenerService {
 public static GlassNotificationListener current;
 @Override public void onListenerConnected(){current=this;}
 @Override public void onListenerDisconnected(){if(current==this)current=null;}
 public static List<StatusBarNotification> recent(){
  List<StatusBarNotification> result=new ArrayList<>();
  try { if(current!=null){StatusBarNotification[] a=current.getActiveNotifications();if(a!=null)for(int i=a.length-1;i>=0&&result.size()<25;i--)result.add(a[i]);}}catch(Exception ignored){}
  return result;
 }
}