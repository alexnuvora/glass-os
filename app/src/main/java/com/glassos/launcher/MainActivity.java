package com.glassos.launcher;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.provider.Settings;
import android.view.*;
import android.view.accessibility.AccessibilityManager;
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
  // Android Canvas approximation of Apple's dynamic glass material.
  // Optical highlights and touch response are real; backdrop refraction is not available here.
  private Drawable translucent(float radius){return new GlassSurface(radius);}
  private class GlassSurface extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float radius;
    private final Path outline=new Path();
    GlassSurface(float r){radius=r;}
    @Override public void draw(Canvas canvas){
      Rect bounds=getBounds();float w=bounds.width(),h=bounds.height();if(w<=0||h<=0)return;
      float r=Math.min(dp(radius),Math.min(w,h)/2f);
      RectF box=new RectF(bounds.left+dp(1),bounds.top+dp(1),bounds.right-dp(1),bounds.bottom-dp(1));
      // Layered tinted translucency with brighter rim at the top edge.
      paint.setStyle(Paint.Style.FILL);
      paint.setShader(new LinearGradient(0,bounds.top,0,bounds.bottom,
        dark?new int[]{0x99b9c4e1,0x557e8aab,0x66818cae}:
             new int[]{0xb7ffffff,0x6ad6e4fa,0x8be8efff},
        new float[]{0f,.44f,1f},Shader.TileMode.CLAMP));
      canvas.drawRoundRect(box,r,r,paint);paint.setShader(null);
      paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1.3f));
      paint.setShader(new LinearGradient(0,bounds.top,0,bounds.bottom,
        new int[]{0xf5ffffff,0x77ffffff,0x44ffffff},null,Shader.TileMode.CLAMP));
      canvas.drawRoundRect(box,r,r,paint);paint.setShader(null);
      // Inner specular arc, clipped to the top portion.
      canvas.save();canvas.clipRect(bounds.left,bounds.top,bounds.right,bounds.top+h*.46f);
      paint.setStrokeWidth(dp(.65f));paint.setColor(0x99ffffff);
      RectF inner=new RectF(box.left+dp(2),box.top+dp(2),box.right-dp(2),box.bottom-dp(2));
      canvas.drawRoundRect(inner,Math.max(0,r-dp(2)),Math.max(0,r-dp(2)),paint);
      canvas.restore();paint.setStyle(Paint.Style.FILL);
    }
    @Override public void setAlpha(int alpha){}
    @Override public void setColorFilter(android.graphics.ColorFilter filter){}
    @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
  }
  private void touch(View v,Runnable action){
    v.setClickable(true);
    v.setOnTouchListener((target,event)->{
      if(event.getAction()==MotionEvent.ACTION_DOWN){
        target.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);target.animate().scaleX(.96f).scaleY(.96f).setDuration(110).start();return true;
      }
      if(event.getAction()==MotionEvent.ACTION_UP){
        target.animate().scaleX(1f).scaleY(1f).setDuration(220).setInterpolator(new android.view.animation.OvershootInterpolator(.6f)).start();
        if(event.getX()>=0&&event.getX()<=target.getWidth()&&event.getY()>=0&&event.getY()<=target.getHeight()) action.run();
        return true;
      }
      if(event.getAction()==MotionEvent.ACTION_CANCEL){
        target.animate().scaleX(1f).scaleY(1f).setDuration(150).start();return true;
      }
      return true;
    });
  }
  private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(1);return v;}
  private LinearLayout horizontal(){LinearLayout v=new LinearLayout(this);v.setOrientation(0);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
  private TextView text(String txt,int size,int color){TextView v=new TextView(this);v.setText(txt);v.setTextColor(color);v.setTextSize(size);v.setGravity(Gravity.CENTER);v.setFontFeatureSettings("kern");return v;}
  private void space(LinearLayout parent,int h){parent.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h)));}
  private void add(LinearLayout host,View child,int w,int h){host.addView(child,new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h)));}
  private void margin(LinearLayout host,View child,int h,int bottom){
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(h));p.bottomMargin=dp(bottom);host.addView(child,p);
  }
  private TextView pill(String label,Runnable action){TextView t=text(label,15,foreground());t.setBackground(translucent(30));touch(t,action);return t;}
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
  private TextView iconFallback(String symbol,int background){
    TextView v=text(symbol,29,Color.WHITE);v.setBackground(shape(background,17));return v;
  }
  private View referenceIcon(String label,String glyph,int color,String... packages){
    LinearLayout tile=column();tile.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
    ResolveInfo installed=match(packages);
    FrameLayout icon=new FrameLayout(this);icon.setBackground(translucent(16));icon.setClipToOutline(true);
    if(installed!=null){ImageView image=new ImageView(this);image.setImageDrawable(installed.loadIcon(pm));image.setScaleType(ImageView.ScaleType.FIT_XY);icon.addView(image,new FrameLayout.LayoutParams(-1,-1));}
    else {TextView fallback=iconFallback(glyph,color);icon.addView(fallback,new FrameLayout.LayoutParams(-1,-1));}
    LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(60),dp(60));tile.addView(icon,ip);
    TextView name=text(label,12,Color.WHITE);name.setSingleLine();name.setEllipsize(TextUtils.TruncateAt.END);name.setShadowLayer(4,0,1,0xaa1f1b20);
    LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,dp(22));np.topMargin=dp(5);tile.addView(name,np);
    tile.setOnClickListener(v->{if(installed!=null)launch(installed);else library();});
    tile.setContentDescription("Open "+label);
    tile.setOnLongClickListener(v->{showAppMenu(v,label,installed);return true;});
    return tile;
  }
  private void showAppMenu(View anchor,String label,ResolveInfo installed){
    PopupMenu menu=new PopupMenu(this,anchor);
    menu.getMenu().add("Open "+label);
    menu.getMenu().add("App info");
    menu.getMenu().add("All apps");
    menu.setOnMenuItemClickListener(item->{
      String choice=item.getTitle().toString();
      if(choice.startsWith("Open")){if(installed!=null)launch(installed);else library();}
      else if(choice.equals("App info") && installed!=null){
        Intent intent=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(android.net.Uri.parse("package:"+installed.activityInfo.packageName));startActivity(intent);
      }else library();
      return true;
    });
    anchor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    menu.show();
  }
  private void enter(View view){
    view.setAlpha(0f);view.setTranslationY(dp(12));
    view.animate().alpha(1f).translationY(0).setDuration(220)
      .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
  }
  private void home(){
    canvas("home");
    shell.setPadding(dp(15),dp(10),dp(15),dp(18));
    TextView status=text(new java.text.SimpleDateFormat("h:mm",Locale.UK).format(new Date())+"                                              ▮▮  ◕  ▰",15,Color.WHITE);
    status.setTypeface(null,1);margin(shell,status,36,12);
    LinearLayout tiles=horizontal();tiles.setGravity(Gravity.TOP);
    LinearLayout weather=column();weather.setPadding(dp(16),dp(15),dp(10),dp(12));
    weather.setBackground(shape(0xff3479b4,29));
    TextView city=text("Weather  ↗",14,WHITE);city.setTypeface(null,1);city.setGravity(Gravity.LEFT);add(weather,city,-1,24);
    TextView temp=text("☀",59,WHITE);temp.setGravity(Gravity.LEFT);add(weather,temp,-1,80);
    TextView condition=text("Weather",16,WHITE);condition.setGravity(Gravity.LEFT);add(weather,condition,-1,30);
    TextView hint=text("Tap to open",12,0xffdceeff);hint.setGravity(Gravity.LEFT);add(weather,hint,-1,23);
    weather.setOnClickListener(v->{ResolveInfo w=match("weather");if(w!=null)launch(w);else library();});
    LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,dp(155),1);wp.rightMargin=dp(9);tiles.addView(weather,wp);
    LinearLayout calendar=column();calendar.setPadding(dp(16),dp(15),dp(10),dp(12));calendar.setBackground(shape(WHITE,29));
    TextView weekday=text(new java.text.SimpleDateFormat("EEEE",Locale.UK).format(new Date()).toUpperCase(Locale.UK),13,0xffee5356);weekday.setGravity(Gravity.LEFT);weekday.setTypeface(null,1);add(calendar,weekday,-1,27);
    TextView date=text(new java.text.SimpleDateFormat("d",Locale.UK).format(new Date()),51,0xff181818);date.setGravity(Gravity.LEFT);add(calendar,date,-1,61);
    TextView events=text("Calendar",16,0xff9c9c9f);events.setGravity(Gravity.LEFT);add(calendar,events,-1,45);
    calendar.setOnClickListener(v->{ResolveInfo app=match("calendar");if(app!=null)launch(app);else library();});
    tiles.addView(calendar,new LinearLayout.LayoutParams(0,dp(155),1));
    add(shell,tiles,-1,155);
    LinearLayout widgetNames=horizontal();
    TextView left=text("Weather",12,WHITE);TextView right=text("Calendar",12,WHITE);
    widgetNames.addView(left,new LinearLayout.LayoutParams(0,dp(30),1));widgetNames.addView(right,new LinearLayout.LayoutParams(0,dp(30),1));add(shell,widgetNames,-1,32);
    space(shell,10);
    String[][] names={
      {"FaceTime","Calendar","Photos","Camera"},
      {"Mail","Notes","Clock","Maps"},
      {"TV","News","App Store","Games"},
      {"Health","Wallet","Siri","Settings"}
    };
    String[][] emojis={
      {"▣","14","✿","◉"},{"✉","▤","◷","➤"},{"tv","N","A","🚀"},{"♥","▰","✦","⚙"}
    };
    int[][] colors={
      {0xff1dd65b,0xffffffff,0xffffffff,0xff707e90},
      {0xff168be9,0xfff6c72b,0xffe8edf3,0xff2abd87},
      {0xff171717,0xfff7f7f7,0xff0a8af0,0xffff4b4f},
      {0xfff9f9f9,0xff222c38,0xffd9dfe5,0xff858993}
    };
    String[][][] packages={
      {{"video","meet"},{"calendar"},{"gallery","photos"},{"camera"}},
      {{"gmail","email"},{"notes","keep"},{"clock"},{"maps"}},
      {{"youtube.tv","tv"},{"news"},{"vending","playstore"},{"games"}},
      {{"health"},{"wallet"},{"assistant","googlequicksearch"},{"settings"}}
    };
    LinearLayout iconRows=column();
    for(int row=0;row<4;row++){
      LinearLayout strip=horizontal();strip.setGravity(Gravity.TOP);
      for(int col=0;col<4;col++){
        final int rr=row,cc=col;
        View item=referenceIcon(names[rr][cc],emojis[rr][cc],colors[rr][cc],packages[rr][cc]);
        strip.addView(item,new LinearLayout.LayoutParams(0,dp(91),1));
      }
      add(iconRows,strip,-1,94);
    }
    shell.addView(iconRows,new LinearLayout.LayoutParams(-1,0,1));
    TextView search=text("⌕  Search",14,0xfff5f5f8);search.setBackground(translucent(30));
    LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(100),dp(35));sp.gravity=Gravity.CENTER;sp.bottomMargin=dp(19);shell.addView(search,sp);touch(search,this::library);
    LinearLayout dock=horizontal();dock.setGravity(Gravity.CENTER);dock.setPadding(dp(9),dp(11),dp(9),dp(8));dock.setBackground(translucent(35));
    String[] dockNames={"Phone","Browser","Messages","Music"};
    String[] dockGlyphs={"☎","◉","●","♫"};int[] dockColors={0xff20d958,0xff238fe8,0xff25d95a,0xffe93453};
    String[][] dockPackages={{"dialer","phone"},{"chrome","browser"},{"messaging","sms"},{"spotify","music"}};
    for(int i=0;i<4;i++){
      ResolveInfo item=match(dockPackages[i]);FrameLayout slot=new FrameLayout(this);
      View icon;
      if(item!=null){ImageView img=new ImageView(this);img.setImageDrawable(item.loadIcon(pm));icon=img;touch(slot,()->launch(item));}
      else{icon=iconFallback(dockGlyphs[i],dockColors[i]);touch(slot,this::library);}
      FrameLayout.LayoutParams frame=new FrameLayout.LayoutParams(dp(61),dp(61),Gravity.CENTER);slot.addView(icon,frame);
      dock.addView(slot,new LinearLayout.LayoutParams(0,dp(67),1));
    }
    add(shell,dock,-1,86);
  }
  private void library(){
    canvas("library");title("App Library");EditText input=new EditText(this);input.setSingleLine();input.setHint("⌕  Search");input.setTextSize(17);input.setPadding(dp(20),0,dp(15),0);input.setBackground(translucent(19));margin(shell,input,52,20);input.setContentDescription("Search installed applications");
    ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);scroll.setFillViewport(true);LinearLayout content=column();scroll.addView(content);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    Runnable update=()->{content.removeAllViews();ArrayList<ResolveInfo> chosen=new ArrayList<>();String q=input.getText().toString().toLowerCase(Locale.ROOT);for(ResolveInfo a:apps)if(a.loadLabel(pm).toString().toLowerCase(Locale.ROOT).contains(q))chosen.add(a);grid(content,chosen);};
    input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){update.run();}public void afterTextChanged(Editable e){}});update.run();enter(scroll);
  }
  private TextView button(String label,Runnable run){
    TextView v=text(label,18,foreground());v.setBackground(translucent(21));touch(v,run);return v;
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
    line("Notifications",this::notifications);line("GlassOS Settings",this::settings);enter(shell);
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
      card.setPadding(dp(16),dp(14),dp(16),dp(14));margin(items,card,100,10);card.setContentDescription("Notification: "+(t==null?n.getPackageName():t.toString()));
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
    line("Refresh installed apps",()->{refresh();home();});
    space(shell,18);
    TextView note=text("GlassOS 0.3  •  iOS-inspired design\n\nThird-party apps and protected Samsung system screens retain their own interface.",13,foreground());add(shell,note,-1,120);
  }
  @Override public void onBackPressed(){if(page.equals("home"))super.onBackPressed();else home();}
}