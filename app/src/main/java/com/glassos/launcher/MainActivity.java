package com.glassos.launcher;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.provider.Settings;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.text.*;
import java.util.*;

public class MainActivity extends Activity {
  private final ArrayList<ResolveInfo> apps=new ArrayList<>();
  private PackageManager pm;
  private LinearLayout shell;
  private String page="home";
  private boolean dark=false, large=false;
  private float downX, downY;
  private final int WHITE=Color.WHITE;
  private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
  private int foreground(){return dark?Color.WHITE:0xff20283b;}
  private int glass(){return dark?0x99727b98:0x99ffffff;}
  private GradientDrawable shape(int color,float radius){
    GradientDrawable x=new GradientDrawable();x.setColor(color);x.setCornerRadius(dp(radius));return x;
  }
  private GradientDrawable translucent(float radius){
    GradientDrawable x=shape(glass(),radius);x.setStroke(dp(1),dark?0x55ffffff:0x99ffffff);return x;
  }
  private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(1);return v;}
  private LinearLayout horizontal(){LinearLayout v=new LinearLayout(this);v.setOrientation(0);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
  private TextView text(String txt,int size,int color){TextView v=new TextView(this);v.setText(txt);v.setTextColor(color);v.setTextSize(size);v.setGravity(Gravity.CENTER);v.setFontFeatureSettings("kern");return v;}
  private void space(LinearLayout parent,int h){parent.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h)));}
  private void add(LinearLayout host,View child,int w,int h){host.addView(child,new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h)));}
  private void margin(LinearLayout host,View child,int h,int bottom){
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(h));p.bottomMargin=dp(bottom);host.addView(child,p);
  }
  private TextView pill(String label,Runnable action){TextView t=text(label,15,foreground());t.setBackground(translucent(30));t.setOnClickListener(v->action.run());return t;}
  @Override public void onCreate(Bundle b){super.onCreate(b);pm=getPackageManager();dark=getPreferences(0).getBoolean("dark",false);large=getPreferences(0).getBoolean("large",false);getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(Color.TRANSPARENT);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);refresh();home();}
  private void refresh(){
    apps.clear();Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);
    apps.addAll(pm.queryIntentActivities(i,0));apps.removeIf(a->getPackageName().equals(a.activityInfo.packageName));
    apps.sort(Comparator.comparing(a->a.loadLabel(pm).toString().toLowerCase(Locale.ROOT)));
  }
  private void launch(ResolveInfo a){
    try{Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);i.setClassName(a.activityInfo.packageName,a.activityInfo.name);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}
    catch(Exception e){Toast.makeText(this,"Can't open this app",Toast.LENGTH_SHORT).show();}
  }
  private void canvas(String id){
    page=id;shell=column();shell.setPadding(dp(18),dp(48),dp(18),dp(25));shell.setBackground(new Wallpaper());setContentView(shell);
  }
  private class Wallpaper extends android.graphics.drawable.Drawable {
    Paint p=new Paint(3);
    @Override public void draw(Canvas c){
      android.graphics.Rect b=getBounds();float w=b.width(),h=b.height();
      p.setShader(new LinearGradient(0,0,w,h,dark?new int[]{0xff17213e,0xff323959,0xff101d33}:new int[]{0xffc9ddf6,0xffe8c6de,0xff9dc8e6},null,Shader.TileMode.CLAMP));c.drawRect(b,p);p.setShader(null);
      glow(c,w*.26f,h*.24f,w*.87f,dark?0x777e6ac7:0xcc9cabf1);
      glow(c,w*.92f,h*.55f,w*.77f,dark?0x776fa2bd:0xbb62d7cf);
      glow(c,w*.23f,h*.9f,w*.7f,dark?0x666a79b8:0xbb8c97df);
    }
    void glow(Canvas c,float x,float y,float r,int color){p.setShader(new RadialGradient(x,y,r,color,Color.TRANSPARENT,Shader.TileMode.CLAMP));c.drawCircle(x,y,r,p);p.setShader(null);}
    @Override public void setAlpha(int a){}@Override public void setColorFilter(android.graphics.ColorFilter f){}@Override public int getOpacity(){return -1;}
  }
  private void title(String name){
    LinearLayout head=horizontal();TextView back=text("‹",37,foreground());add(head,back,44,56);back.setOnClickListener(v->home());
    TextView t=text(name,29,foreground());t.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);
    head.addView(t,new LinearLayout.LayoutParams(0,dp(56),1));add(shell,head,-1,58);space(shell,18);
  }
  private View appTile(ResolveInfo info){
    LinearLayout tile=column();tile.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
    FrameLayout iconWrap=new FrameLayout(this);iconWrap.setBackground(translucent(17));iconWrap.setClipToOutline(true);
    ImageView image=new ImageView(this);image.setImageDrawable(info.loadIcon(pm));image.setScaleType(ImageView.ScaleType.FIT_CENTER);
    FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(-1,-1);ip.setMargins(dp(2),dp(2),dp(2),dp(2));iconWrap.addView(image,ip);
    add(tile,iconWrap,large?70:61,large?70:61);
    if(!large){TextView caption=text(info.loadLabel(pm).toString(),11,dark?WHITE:0xff263b5b);caption.setSingleLine(true);caption.setEllipsize(TextUtils.TruncateAt.END);caption.setShadowLayer(2,0,1,0x66ffffff);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(22));lp.topMargin=dp(5);tile.addView(caption,lp);}
    tile.setOnClickListener(v->launch(info));return tile;
  }
  private void grid(LinearLayout into,ArrayList<ResolveInfo> list){
    for(int index=0;index<list.size();index+=4){
      LinearLayout r=horizontal();r.setGravity(Gravity.TOP);
      for(int j=index;j<index+4;j++){
        View v=j<list.size()?appTile(list.get(j)):new View(this);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(large?95:99),1);lp.leftMargin=dp(2);lp.rightMargin=dp(2);r.addView(v,lp);
      }
      add(into,r,-1,large?96:102);
    }
  }
  private ResolveInfo match(String... terms){
    for(String term:terms)for(ResolveInfo app:apps)if(app.activityInfo.packageName.toLowerCase(Locale.ROOT).contains(term))return app;
    return null;
  }
  private void home(){
    canvas("home");
    LinearLayout top=horizontal();TextView clock=text(new java.text.SimpleDateFormat("HH:mm",Locale.UK).format(new Date()),17,foreground());clock.setTypeface(null,1);
    top.addView(clock,new LinearLayout.LayoutParams(0,dp(35),1));
    TextView controls=text("◉  ☷",22,foreground());add(top,controls,72,35);controls.setOnClickListener(v->controls());
    add(shell,top,-1,35);space(shell,22);
    LinearLayout widgets=horizontal();
    LinearLayout date=column();date.setPadding(dp(17),dp(13),dp(12),dp(12));date.setBackground(translucent(26));
    TextView day=text(new java.text.SimpleDateFormat("EEEE",Locale.UK).format(new Date()).toUpperCase(Locale.UK),11,0xffe4534f);day.setGravity(Gravity.LEFT);add(date,day,-1,23);
    TextView number=text(new java.text.SimpleDateFormat("d",Locale.UK).format(new Date()),51,foreground());number.setGravity(Gravity.LEFT);add(date,number,-1,66);
    TextView month=text(new java.text.SimpleDateFormat("MMMM yyyy",Locale.UK).format(new Date()),13,foreground());month.setGravity(Gravity.LEFT);add(date,month,-1,27);
    LinearLayout.LayoutParams wl=new LinearLayout.LayoutParams(0,dp(154),1);wl.rightMargin=dp(12);widgets.addView(date,wl);
    LinearLayout action=column();action.setPadding(dp(14),dp(16),dp(14),dp(12));action.setBackground(translucent(26));
    TextView label=text("GLASSOS",12,foreground());label.setGravity(Gravity.LEFT);add(action,label,-1,30);
    TextView middle=text("Good "+(new java.util.Date().getHours()<12?"morning":new java.util.Date().getHours()<18?"afternoon":"evening"),19,foreground());middle.setGravity(Gravity.LEFT);add(action,middle,-1,46);
    TextView hint=text("Swipe up for apps  ↗",12,foreground());hint.setGravity(Gravity.LEFT);add(action,hint,-1,30);
    widgets.addView(action,new LinearLayout.LayoutParams(0,dp(154),1));action.setOnClickListener(v->library());
    add(shell,widgets,-1,154);space(shell,22);
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
    LinearLayout area=column();area.setGravity(Gravity.TOP);ArrayList<ResolveInfo> visible=new ArrayList<>();
    for(ResolveInfo a:apps){if(visible.size()==16)break;if(!a.activityInfo.packageName.contains("com.android.settings"))visible.add(a);}
    grid(area,visible);scroll.addView(area);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    TextView search=pill("⌕   Search",this::library);
    LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(132),dp(35));sp.gravity=Gravity.CENTER;sp.bottomMargin=dp(13);shell.addView(search,sp);
    LinearLayout dock=horizontal();dock.setPadding(dp(9),dp(12),dp(9),dp(3));dock.setBackground(translucent(32));
    String[][] preferences={{"dialer","phone"},{"messaging","sms"},{"chrome","browser"},{"camera"}};
    for(String[] options:preferences){
      ResolveInfo app=match(options);if(app==null&&!apps.isEmpty())app=apps.get(Math.min(dock.getChildCount(),apps.size()-1));
      FrameLayout holder=new FrameLayout(this);if(app!=null){ImageView img=new ImageView(this);img.setImageDrawable(app.loadIcon(pm));FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(dp(57),dp(57),Gravity.CENTER);holder.addView(img,ip);ResolveInfo selected=app;holder.setOnClickListener(v->launch(selected));}
      dock.addView(holder,new LinearLayout.LayoutParams(0,dp(64),1));
    }
    add(shell,dock,-1,81);
    shell.setOnTouchListener((v,e)->{if(e.getAction()==0){downY=e.getY();downX=e.getX();return true;}if(e.getAction()==1){float dy=e.getY()-downY;if(dy < -dp(45))library();else if(dy>dp(55))controls();return true;}return true;});
  }
  private void library(){
    canvas("library");title("App Library");EditText input=new EditText(this);input.setSingleLine();input.setHint("⌕  Search");input.setTextSize(17);input.setPadding(dp(20),0,dp(15),0);input.setBackground(translucent(19));margin(shell,input,52,20);
    ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);LinearLayout content=column();scroll.addView(content);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    Runnable update=()->{content.removeAllViews();ArrayList<ResolveInfo> chosen=new ArrayList<>();String q=input.getText().toString().toLowerCase(Locale.ROOT);for(ResolveInfo a:apps)if(a.loadLabel(pm).toString().toLowerCase(Locale.ROOT).contains(q))chosen.add(a);grid(content,chosen);};
    input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){update.run();}public void afterTextChanged(Editable e){}});update.run();
  }
  private TextView button(String label,Runnable run){
    TextView v=text(label,18,foreground());v.setBackground(translucent(21));v.setOnClickListener(w->run.run());return v;
  }
  private void line(String title,Runnable run){TextView b=button(title+"    ›",run);b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);b.setPadding(dp(20),0,dp(14),0);margin(shell,b,57,12);}
  private void controls(){
    canvas("controls");title("Control Centre");
    LinearLayout top=horizontal();
    TextView wifi=button("●\nWi-Fi",()->startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));top.addView(wifi,new LinearLayout.LayoutParams(0,dp(125),1));
    TextView bluetooth=button("ᛒ\nBluetooth",()->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(125),1);bp.leftMargin=dp(12);top.addView(bluetooth,bp);add(shell,top,-1,125);space(shell,12);
    TextView media=button("♪       Not Playing\n\n            ▶",()->startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS)));margin(shell,media,132,12);
    line("Focus & sound",()->startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS)));
    TextView brightness=text("☀   Brightness",19,foreground());add(shell,brightness,-1,46);
    SeekBar slider=new SeekBar(this);slider.setMax(255);slider.setProgress(155);slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
      public void onProgressChanged(SeekBar seek,int progress,boolean fromUser){if(fromUser){WindowManager.LayoutParams a=getWindow().getAttributes();a.screenBrightness=Math.max(.02f,progress/255f);getWindow().setAttributes(a);}}
      public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}
    });add(shell,slider,-1,58);
    line("Notifications",this::notifications);line("GlassOS Settings",this::settings);
  }
  private void notifications(){
    canvas("notifications");title("Notifications");
    if(GlassNotificationListener.current==null){line("Enable notification access",()->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));return;}
    ScrollView scroll=new ScrollView(this);LinearLayout items=column();scroll.addView(items);shell.addView(scroll);
    java.util.List<android.service.notification.StatusBarNotification> ns=GlassNotificationListener.recent();
    if(ns.isEmpty())add(items,text("No new notifications",19,foreground()),-1,100);
    for(android.service.notification.StatusBarNotification n:ns){
      CharSequence t=n.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE);
      CharSequence msg=n.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT);
      TextView card=button((t==null?n.getPackageName():t.toString())+"\n"+(msg==null?"":msg),()->{try{if(n.getNotification().contentIntent!=null)n.getNotification().contentIntent.send();}catch(Exception ignored){}});
      card.setPadding(dp(16),dp(14),dp(16),dp(14));margin(items,card,100,10);
    }
  }
  private void settings(){
    canvas("settings");title("GlassOS Settings");
    TextView section=text("APPEARANCE",12,foreground());section.setGravity(Gravity.LEFT);margin(shell,section,38,8);
    line("Appearance: "+(dark?"Dark":"Light"),()->{dark=!dark;getPreferences(0).edit().putBoolean("dark",dark).apply();settings();});
    line("App icons: "+(large?"Large":"Standard"),()->{large=!large;getPreferences(0).edit().putBoolean("large",large).apply();settings();});
    TextView section2=text("ANDROID INTEGRATION",12,foreground());section2.setGravity(Gravity.LEFT);margin(shell,section2,48,5);
    line("Select default Home app",()->startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)));
    line("Notification access",()->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
    line("Android system settings",()->startActivity(new Intent(Settings.ACTION_SETTINGS)));
    space(shell,18);
    TextView note=text("GlassOS 0.2  •  iOS-inspired design\n\nThird-party apps and protected Samsung system screens retain their own interface.",13,foreground());add(shell,note,-1,120);
  }
  @Override public void onBackPressed(){if(page.equals("home"))super.onBackPressed();else home();}
}