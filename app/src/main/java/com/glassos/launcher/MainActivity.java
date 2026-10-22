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
  private boolean dark=false, large=false, reduceMotion=false;
  private int wallpaperStyle=0;
  private boolean showSeconds=false;
  private boolean highContrastGlass=false;
  private float glassStrength=0.70f;
  private final String[] wallpaperNames={"Sky","Midnight","Sunset","Ocean"};
  private Bitmap blurredBackdrop;
  private int iconTint=0;
  private int widgetMode=0;
  private boolean hidePrivateNotifications=false;
  private final String[] tintLabels={"Original","Monochrome","Soft tint"};
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
  private Drawable translucent(float radius){return new GlassSurface(radius,null);}
  // Centralised motion timing so interactions share a consistent rhythm.
  private long motionDuration(int nominal){return reduceMotion?0:nominal;}
  private android.view.animation.Interpolator spring(){return new android.view.animation.OvershootInterpolator(.55f);}
  private void reveal(View view,int delay){
    if(reduceMotion)return;
    view.setAlpha(0f);view.setTranslationY(dp(18));view.setScaleX(.975f);view.setScaleY(.975f);
    view.animate().alpha(1f).translationY(0).scaleX(1f).scaleY(1f)
      .setStartDelay(delay).setDuration(340).setInterpolator(new android.view.animation.DecelerateInterpolator(2f)).start();
  }
  private void setGlass(View v,float radius){v.setBackground(new GlassSurface(radius,v));}
  private Bitmap backdrop(){
    int width=Math.max(1,getResources().getDisplayMetrics().widthPixels);
    int height=Math.max(1,getResources().getDisplayMetrics().heightPixels);
    if(blurredBackdrop!=null)return blurredBackdrop;
    Bitmap original=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
    Wallpaper wallpaper=new Wallpaper();wallpaper.setBounds(0,0,width,height);wallpaper.draw(new Canvas(original));
    int smallWidth=Math.max(1,width/18),smallHeight=Math.max(1,height/18);
    Bitmap reduced=Bitmap.createScaledBitmap(original,smallWidth,smallHeight,true);
    blurredBackdrop=Bitmap.createScaledBitmap(reduced,width,height,true);
    original.recycle();reduced.recycle();return blurredBackdrop;
  }
  private class GlassSurface extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float radius;
    private final Path outline=new Path();
    private final View anchor;
    GlassSurface(float r,View owner){radius=r;anchor=owner;}
    @Override public void draw(Canvas canvas){
      Rect bounds=getBounds();float w=bounds.width(),h=bounds.height();if(w<=0||h<=0)return;
      float r=Math.min(dp(radius),Math.min(w,h)/2f);
      RectF box=new RectF(bounds.left+dp(1),bounds.top+dp(1),bounds.right-dp(1),bounds.bottom-dp(1));
      // Sample the actual launcher wallpaper as a low-pass filtered background,
      // with a small displacement for an optical lens-like shift.
      if(anchor!=null){
        Bitmap image=backdrop();
        BitmapShader shader=new BitmapShader(image,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);
        int[] pos=new int[2];anchor.getLocationOnScreen(pos);
        Matrix matrix=new Matrix();// Small spatial offset gives a lensing cue without capturing other app content.
        matrix.setTranslate(-pos[0]+dp(3),-pos[1]+dp(4));shader.setLocalMatrix(matrix);
        canvas.save();
        Path clip=new Path();clip.addRoundRect(box,r,r,Path.Direction.CW);canvas.clipPath(clip);
        paint.setStyle(Paint.Style.FILL);paint.setShader(shader);canvas.drawRect(box,paint);
        paint.setShader(null);canvas.restore();
      }
      // Layered tinted translucency with brighter rim at the top edge.
      paint.setStyle(Paint.Style.FILL);
      paint.setAlpha(255);
      paint.setShader(new LinearGradient(0,bounds.top,0,bounds.bottom,
        dark?new int[]{0x88b9c4e1,0x447e8aab,0x55818cae}:
             new int[]{highContrastGlass?0xbbffffff:0x55ffffff,0x24d6e4fa,highContrastGlass?0x99e8efff:0x55e8efff},
        new float[]{0f,.44f,1f},Shader.TileMode.CLAMP));
      paint.setAlpha((int)(255*glassStrength));canvas.drawRoundRect(box,r,r,paint);paint.setShader(null);paint.setAlpha(255);
      paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1.3f));
      paint.setShader(new LinearGradient(0,bounds.top,0,bounds.bottom,
        new int[]{0xf5ffffff,0x77ffffff,0x44ffffff},null,Shader.TileMode.CLAMP));
      canvas.drawRoundRect(box,r,r,paint);paint.setShader(null);
      // Faint lower lens shadow and a second soft edge create separation.
      paint.setShader(null);paint.setColor(dark?0x550a102a:0x30415885);
      paint.setStrokeWidth(dp(1.7f));
      canvas.drawArc(new RectF(box.left+dp(1),box.top+dp(1),box.right-dp(1),box.bottom-dp(1)),10,160,false,paint);
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
        target.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);if(reduceMotion)return true;target.animate().scaleX(.955f).scaleY(.955f).alpha(.88f).setDuration(110).start();return true;
      }
      if(event.getAction()==MotionEvent.ACTION_UP){
        target.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(motionDuration(280)).setInterpolator(spring()).start();
        if(event.getX()>=0&&event.getX()<=target.getWidth()&&event.getY()>=0&&event.getY()<=target.getHeight()) action.run();
        return true;
      }
      if(event.getAction()==MotionEvent.ACTION_CANCEL){
        target.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(motionDuration(150)).start();return true;
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
  private TextView pill(String label,Runnable action){TextView t=text(label,15,foreground());setGlass(t,30);touch(t,action);return t;}
  @Override public void onCreate(Bundle b){super.onCreate(b);pm=getPackageManager();dark=getPreferences(0).getBoolean("dark",false);large=getPreferences(0).getBoolean("large",false);reduceMotion=getPreferences(0).getBoolean("reduceMotion",false);wallpaperStyle=getPreferences(0).getInt("wallpaperStyle",0);iconTint=getPreferences(0).getInt("iconTint",0);widgetMode=getPreferences(0).getInt("widgetMode",0);hidePrivateNotifications=getPreferences(0).getBoolean("hidePrivateNotifications",false);showSeconds=getPreferences(0).getBoolean("showSeconds",false);highContrastGlass=getPreferences(0).getBoolean("highContrastGlass",false);glassStrength=getPreferences(0).getFloat("glassStrength",.70f);getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(Color.TRANSPARENT);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);refresh();home();}
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
    page=id;shell=column();shell.setPadding(dp(18),dp(48),dp(18),dp(25));shell.setBackground(new Wallpaper());setContentView(shell);if(!id.equals("home"))enter(shell);
  }
  private class Wallpaper extends android.graphics.drawable.Drawable {
    Paint p=new Paint(3);
    @Override public void draw(Canvas c){
      android.graphics.Rect b=getBounds();float w=b.width(),h=b.height();
      p.setShader(new LinearGradient(0,0,w,h,wallpaperStyle==1?new int[]{0xff121c32,0xff303c67,0xff15172e}:wallpaperStyle==2?new int[]{0xfff3a1a4,0xffba7ec8,0xff604faa}:wallpaperStyle==3?new int[]{0xff216eac,0xff33b2b5,0xff20407b}:(dark?new int[]{0xff17213e,0xff323959,0xff101d33}:new int[]{0xffc9ddf6,0xffe8c6de,0xff9dc8e6}),null,Shader.TileMode.CLAMP));c.drawRect(b,p);p.setShader(null);
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
    FrameLayout iconWrap=new FrameLayout(this);setGlass(iconWrap,17);iconWrap.setClipToOutline(true);
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
  private void applyIconTint(ImageView view){
    if(iconTint==1){ColorMatrix matrix=new ColorMatrix();matrix.setSaturation(0);view.setColorFilter(new ColorMatrixColorFilter(matrix));}
    else if(iconTint==2){ColorMatrix matrix=new ColorMatrix();matrix.setSaturation(.5f);view.setColorFilter(new ColorMatrixColorFilter(matrix));}
  }
  private View referenceIcon(String label,String glyph,int color,int index,String... packages){
    LinearLayout tile=column();tile.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
    ResolveInfo installed=savedApp(index,packages);
    String folder=getPreferences(0).getString("folder_"+index,"");
    String customLabel=getPreferences(0).getString("label_"+index,"");
    final String tileLabel=folder.isEmpty()?label:"Folder";
    FrameLayout icon=new FrameLayout(this);setGlass(icon,16);icon.setClipToOutline(true);
    if(folder.length()>0){TextView fIcon=iconFallback("▦",0x996c87c1);icon.addView(fIcon,new FrameLayout.LayoutParams(-1,-1));}
    else if(installed!=null){ImageView image=new ImageView(this);image.setImageDrawable(installed.loadIcon(pm));image.setScaleType(ImageView.ScaleType.FIT_XY);
      applyIconTint(image);icon.addView(image,new FrameLayout.LayoutParams(-1,-1));}
    else {TextView fallback=iconFallback(glyph,color);icon.addView(fallback,new FrameLayout.LayoutParams(-1,-1));}
    LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(60),dp(60));tile.addView(icon,ip);
    TextView name=text(!customLabel.isEmpty()?customLabel:(folder.isEmpty() && installed!=null?installed.loadLabel(pm).toString():tileLabel),12,Color.WHITE);name.setSingleLine();name.setEllipsize(TextUtils.TruncateAt.END);name.setShadowLayer(4,0,1,0xaa1f1b20);
    LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,dp(22));np.topMargin=dp(5);tile.addView(name,np);
    tile.setOnClickListener(v->{if(!folder.isEmpty())openFolder(index);else if(installed!=null)launch(installed);else library();});
    tile.setContentDescription("Open "+tileLabel);
    tile.setOnLongClickListener(v->{showAppMenu(v,tileLabel,installed,index);return true;});
    return tile;
  }
  private void showAppMenu(View anchor,String label,ResolveInfo installed,int index){
    PopupMenu menu=new PopupMenu(this,anchor);
    menu.getMenu().add("Open "+label);
    menu.getMenu().add("App info");
    menu.getMenu().add("All apps");
    menu.getMenu().add("Home screen settings");
    menu.getMenu().add("Change app in this position");
    menu.getMenu().add("Rename icon");
    if(index<16)menu.getMenu().add("Create or edit folder");
    if(index<16)menu.getMenu().add("Move icon");
    menu.setOnMenuItemClickListener(item->{
      String choice=item.getTitle().toString();
      if(choice.startsWith("Open")){if(installed!=null)launch(installed);else library();}
      else if(choice.equals("App info") && installed!=null){
        Intent intent=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(android.net.Uri.parse("package:"+installed.activityInfo.packageName));startActivity(intent);
      }else if(choice.equals("Home screen settings"))settings();else if(choice.equals("Change app in this position"))pickApp(index);else if(choice.equals("Rename icon"))renameIcon(index,label);else if(choice.equals("Create or edit folder"))editFolder(index);else if(choice.equals("Move icon"))moveIcon(index);else library();
      return true;
    });
    anchor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    menu.show();
  }
  private void enter(View view){
    if(reduceMotion)return;view.setAlpha(0f);view.setTranslationY(dp(12));
    view.animate().alpha(1f).translationY(0).setDuration(220)
      .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
  }
  private ResolveInfo savedApp(int position,String... fallbacks){
    String pkg=getPreferences(0).getString("slot_"+position,"");
    if(!pkg.isEmpty())for(ResolveInfo a:apps)if(a.activityInfo.packageName.equals(pkg))return a;
    return match(fallbacks);
  }
  private void pickApp(int position){
    refresh();
    final String[] titles=new String[apps.size()+1];
    titles[0]="Restore default";
    for(int i=0;i<apps.size();i++)titles[i+1]=apps.get(i).loadLabel(pm).toString();
    new AlertDialog.Builder(this).setTitle(position>=16?"Choose dock app":"Choose home-screen app")
      .setItems(titles,(dialog,which)->{
        android.content.SharedPreferences.Editor e=getPreferences(0).edit();
        e.putString("slot_"+position,which==0?"":apps.get(which-1).activityInfo.packageName).apply();
        home();
      }).setNegativeButton("Cancel",null).show();
  }
  private void chooseWallpaper(){
    new AlertDialog.Builder(this).setTitle("Wallpaper")
      .setSingleChoiceItems(wallpaperNames,wallpaperStyle,(dialog,index)->{
        wallpaperStyle=index;blurredBackdrop=null;getPreferences(0).edit().putInt("wallpaperStyle",index).apply();
        dialog.dismiss();settings();
      }).setNegativeButton("Cancel",null).show();
  }
  private void editHome(){
    final String[] choices={"Select home screen position","Choose folder contents","Choose icon appearance","Wallpaper","Reset arrangement"};
    new AlertDialog.Builder(this).setTitle("Customise Home Screen").setItems(choices,(dialog,which)->{
      if(which==0)choosePosition(false);
      else if(which==1)choosePosition(true);
      else if(which==2)iconAppearance();
      else if(which==3)chooseWallpaper();
      else resetLayout();
    }).setNegativeButton("Close",null).show();
  }
  private void choosePosition(boolean folder){
    String[] slots=new String[16];for(int i=0;i<16;i++)slots[i]="Icon "+(i+1);
    new AlertDialog.Builder(this).setTitle("Choose position").setItems(slots,(dialog,which)->{
      if(folder)editFolder(which);else pickApp(which);
    }).setNegativeButton("Cancel",null).show();
  }
  private void chooseGlassStrength(){
    final SeekBar slider=new SeekBar(this);slider.setMax(90);slider.setProgress(Math.round(glassStrength*100)-10);
    LinearLayout panel=column();panel.setPadding(dp(24),dp(15),dp(24),0);
    TextView label=text("Glass material opacity",16,foreground());add(panel,label,-1,42);add(panel,slider,-1,56);
    new AlertDialog.Builder(this).setTitle("Liquid Glass appearance").setView(panel)
      .setNegativeButton("Cancel",null)
      .setPositiveButton("Apply",(dialog,which)->{
        glassStrength=(slider.getProgress()+10)/100f;
        getPreferences(0).edit().putFloat("glassStrength",glassStrength).apply();settings();
      }).show();
  }
  private void renameIcon(int position,String oldName){
    EditText input=new EditText(this);input.setSingleLine(true);
    input.setText(getPreferences(0).getString("label_"+position,oldName));input.selectAll();
    LinearLayout holder=column();holder.setPadding(dp(24),dp(8),dp(24),0);add(holder,input,-1,55);
    new AlertDialog.Builder(this).setTitle("Rename Home Screen icon")
      .setView(holder).setNegativeButton("Cancel",null)
      .setNeutralButton("Use app name",(dialog,which)->{getPreferences(0).edit().remove("label_"+position).apply();home();})
      .setPositiveButton("Save",(dialog,which)->{getPreferences(0).edit().putString("label_"+position,input.getText().toString().trim()).apply();home();}).show();
  }
  private void chooseWidget(){
    new AlertDialog.Builder(this).setTitle("Top left widget")
      .setSingleChoiceItems(new String[]{"Battery percentage","Digital clock"},widgetMode,(dialog,which)->{
        widgetMode=which;getPreferences(0).edit().putInt("widgetMode",which).apply();dialog.dismiss();home();
      }).setNegativeButton("Cancel",null).show();
  }
  private void resetLayout(){
    new AlertDialog.Builder(this).setTitle("Reset Home Screen?")
      .setMessage("Restore the default app positions and dock?")
      .setNegativeButton("Cancel",null)
      .setPositiveButton("Reset",(dialog,which)->{
        android.content.SharedPreferences.Editor edit=getPreferences(0).edit();
        for(int i=0;i<20;i++){edit.remove("slot_"+i);edit.remove("folder_"+i);edit.remove("label_"+i);}
        edit.apply();home();
      }).show();
  }
  private ResolveInfo byPackage(String pkg){
    for(ResolveInfo app:apps)if(app.activityInfo.packageName.equals(pkg))return app;
    return null;
  }
  private void editFolder(int position){
    String existing=getPreferences(0).getString("folder_"+position,"");
    boolean[] selected=new boolean[apps.size()];
    ArrayList<String> paths=new ArrayList<>();
    if(!existing.isEmpty())paths.addAll(Arrays.asList(existing.split(",")));
    String[] names=new String[apps.size()];
    for(int i=0;i<apps.size();i++){names[i]=apps.get(i).loadLabel(pm).toString();selected[i]=paths.contains(apps.get(i).activityInfo.packageName);}
    new AlertDialog.Builder(this).setTitle("Folder apps")
      .setMultiChoiceItems(names,selected,(dialog,which,checked)->selected[which]=checked)
      .setNeutralButton("Remove folder",(dialog,which)->{getPreferences(0).edit().remove("folder_"+position).apply();home();})
      .setNegativeButton("Cancel",null)
      .setPositiveButton("Save",(dialog,which)->{
        StringBuilder combined=new StringBuilder();
        for(int i=0;i<selected.length;i++)if(selected[i]){
          if(combined.length()>0)combined.append(",");
          combined.append(apps.get(i).activityInfo.packageName);
        }
        getPreferences(0).edit().putString("folder_"+position,combined.toString()).apply();home();
      }).show();
  }
  private void openFolder(int position){
    String contents=getPreferences(0).getString("folder_"+position,"");
    ArrayList<ResolveInfo> members=new ArrayList<>();
    for(String pkg:contents.split(",")){ResolveInfo app=byPackage(pkg);if(app!=null)members.add(app);}
    if(members.isEmpty()){editFolder(position);return;}
    final Dialog dialog=new Dialog(this);
    LinearLayout panel=column();panel.setPadding(dp(20),dp(18),dp(20),dp(18));
    setGlass(panel,32);
    TextView heading=text("Folder",24,foreground());heading.setGravity(Gravity.CENTER);add(panel,heading,-1,56);
    for(int start=0;start<members.size();start+=3){
      LinearLayout strip=horizontal();strip.setGravity(Gravity.TOP);
      for(int j=start;j<start+3;j++){
        if(j>=members.size()){strip.addView(new View(this),new LinearLayout.LayoutParams(0,dp(100),1));continue;}
        ResolveInfo app=members.get(j);
        LinearLayout tile=column();tile.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView icon=new ImageView(this);icon.setImageDrawable(app.loadIcon(pm));applyIconTint(icon);
        add(tile,icon,61,61);
        TextView name=text(app.loadLabel(pm).toString(),11,foreground());name.setSingleLine(true);name.setEllipsize(TextUtils.TruncateAt.END);
        add(tile,name,-1,27);
        touch(tile,()->{dialog.dismiss();launch(app);});
        strip.addView(tile,new LinearLayout.LayoutParams(0,dp(101),1));
      }
      add(panel,strip,-1,105);
    }
    TextView edit=button("Edit folder",()->{dialog.dismiss();editFolder(position);});margin(panel,edit,48,0);
    dialog.setContentView(panel);
    Window win=dialog.getWindow();
    if(win!=null){win.setBackgroundDrawableResource(android.R.color.transparent);win.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.91),-2);win.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams params=win.getAttributes();params.dimAmount=.36f;win.setAttributes(params);}
    dialog.show();
    if(dialog.getWindow()!=null)dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*.91),-2);
    if(!reduceMotion){panel.setScaleX(.86f);panel.setScaleY(.86f);panel.setAlpha(0f);panel.animate().alpha(1f).scaleX(1).scaleY(1).setDuration(300).setInterpolator(new android.view.animation.OvershootInterpolator(.65f)).start();}
  }
    private void moveIcon(int position){
    String[] slots=new String[16];for(int i=0;i<16;i++)slots[i]="Position "+(i+1);
    new AlertDialog.Builder(this).setTitle("Move to")
      .setItems(slots,(dialog,target)->{
        if(target==position)return;
        android.content.SharedPreferences prefs=getPreferences(0);
        String a=prefs.getString("slot_"+position,""),b=prefs.getString("slot_"+target,"");
        String fa=prefs.getString("folder_"+position,""),fb=prefs.getString("folder_"+target,"");
        prefs.edit().putString("slot_"+position,b).putString("slot_"+target,a)
          .putString("folder_"+position,fb).putString("folder_"+target,fa).apply();home();
      }).setNegativeButton("Cancel",null).show();
  }
  private void iconAppearance(){
    new AlertDialog.Builder(this).setTitle("Icon appearance")
      .setSingleChoiceItems(tintLabels,iconTint,(dialog,which)->{
        iconTint=which;getPreferences(0).edit().putInt("iconTint",which).apply();dialog.dismiss();home();
      }).setNegativeButton("Cancel",null).show();
  }
  private void home(){
    canvas("home");
    shell.setPadding(dp(15),dp(10),dp(15),dp(18));
    TextView status=text(new java.text.SimpleDateFormat(showSeconds?"h:mm:ss":"h:mm",Locale.UK).format(new Date())+"        ●●●  ▰",15,Color.WHITE);
    status.setTypeface(null,1);margin(shell,status,36,12);
    LinearLayout tiles=horizontal();tiles.setGravity(Gravity.TOP);
    LinearLayout weather=column();weather.setPadding(dp(16),dp(15),dp(10),dp(12));
    weather.setBackground(shape(0xff3479b4,29));
    TextView city=text(widgetMode==1?"CLOCK  ↗":"BATTERY  ↗",14,WHITE);city.setTypeface(null,1);city.setGravity(Gravity.LEFT);add(weather,city,-1,24);
    android.os.BatteryManager battery=(android.os.BatteryManager)getSystemService(BATTERY_SERVICE);
    int charge=battery==null?-1:battery.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
    TextView temp=text(widgetMode==1?new java.text.SimpleDateFormat("HH:mm",Locale.UK).format(new Date()):(charge>=0?charge+"%":"–"),47,WHITE);temp.setGravity(Gravity.LEFT);add(weather,temp,-1,80);
    TextView condition=text(widgetMode==1?"Local time":"Battery",16,WHITE);condition.setGravity(Gravity.LEFT);add(weather,condition,-1,30);
    TextView hint=text(widgetMode==1?"United Kingdom":"Device status",12,0xffdceeff);hint.setGravity(Gravity.LEFT);add(weather,hint,-1,23);
    weather.setOnClickListener(v->chooseWidget());
    weather.setOnLongClickListener(v->{chooseWidget();return true;});
    LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,dp(155),1);wp.rightMargin=dp(9);tiles.addView(weather,wp);
    LinearLayout calendar=column();calendar.setPadding(dp(16),dp(15),dp(10),dp(12));calendar.setBackground(shape(WHITE,29));
    TextView weekday=text(new java.text.SimpleDateFormat("EEEE",Locale.UK).format(new Date()).toUpperCase(Locale.UK),13,0xffee5356);weekday.setGravity(Gravity.LEFT);weekday.setTypeface(null,1);add(calendar,weekday,-1,27);
    TextView date=text(new java.text.SimpleDateFormat("d",Locale.UK).format(new Date()),51,0xff181818);date.setGravity(Gravity.LEFT);add(calendar,date,-1,61);
    TextView events=text("Calendar",16,0xff9c9c9f);events.setGravity(Gravity.LEFT);add(calendar,events,-1,45);
    calendar.setOnClickListener(v->{ResolveInfo app=match("calendar");if(app!=null)launch(app);else library();});
    tiles.addView(calendar,new LinearLayout.LayoutParams(0,dp(155),1));
    add(shell,tiles,-1,155);reveal(tiles,70);
    LinearLayout widgetNames=horizontal();
    TextView left=text(widgetMode==1?"Clock":"Battery",12,WHITE);TextView right=text("Calendar",12,WHITE);
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
        View item=referenceIcon(names[rr][cc],emojis[rr][cc],colors[rr][cc],rr*4+cc,packages[rr][cc]);
        strip.addView(item,new LinearLayout.LayoutParams(0,dp(91),1));
      }
      add(iconRows,strip,-1,94);
    }
    shell.addView(iconRows,new LinearLayout.LayoutParams(-1,0,1));reveal(iconRows,130);
    TextView search=text("⌕  Search",14,0xfff5f5f8);setGlass(search,30);
    LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(100),dp(35));sp.gravity=Gravity.CENTER;sp.bottomMargin=dp(19);shell.addView(search,sp);touch(search,this::library);
    LinearLayout dock=horizontal();dock.setGravity(Gravity.CENTER);dock.setPadding(dp(9),dp(11),dp(9),dp(8));setGlass(dock,35);
    String[] dockNames={"Phone","Browser","Messages","Music"};
    String[] dockGlyphs={"☎","◉","●","♫"};int[] dockColors={0xff20d958,0xff238fe8,0xff25d95a,0xffe93453};
    String[][] dockPackages={{"dialer","phone"},{"chrome","browser"},{"messaging","sms"},{"spotify","music"}};
    for(int i=0;i<4;i++){
      ResolveInfo item=savedApp(16+i,dockPackages[i]);final int dockIndex=16+i;FrameLayout slot=new FrameLayout(this);
      View icon;
      if(item!=null){ImageView img=new ImageView(this);img.setImageDrawable(item.loadIcon(pm));applyIconTint(img);icon=img;touch(slot,()->launch(item));}
      else{icon=iconFallback(dockGlyphs[i],dockColors[i]);touch(slot,this::library);}
      FrameLayout.LayoutParams frame=new FrameLayout.LayoutParams(dp(61),dp(61),Gravity.CENTER);slot.addView(icon,frame);
      slot.setOnLongClickListener(v->{pickApp(dockIndex);return true;});
      dock.addView(slot,new LinearLayout.LayoutParams(0,dp(67),1));
    }
    add(shell,dock,-1,86);reveal(dock,180);
    shell.setFocusableInTouchMode(true);
    shell.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();return true;}if(e.getAction()==MotionEvent.ACTION_UP){float dx=e.getX()-downX,dy=e.getY()-downY;if(dy>dp(65))controls();else if(dy< -dp(65)||dx< -dp(95))library();else if(dx>dp(95))notifications();return true;}return true;});
  }
  private void library(){
    refresh();canvas("library");title("App Library");
    EditText input=new EditText(this);input.setSingleLine();input.setHint("Search apps and settings");
    input.setTextSize(17);input.setPadding(dp(18),0,dp(15),0);setGlass(input,19);
    margin(shell,input,52,12);input.setContentDescription("Search installed applications");
    LinearLayout filterBar=horizontal();
    final String[] filters={"All","Favorites","A–M","N–Z"};
    LinearLayout results=column();
    ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);
    scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);scroll.addView(results);shell.addView(filterBar,new LinearLayout.LayoutParams(-1,dp(48)));
    shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    final int[] selected={0};
    Runnable update=()->{
      results.removeAllViews();
      String q=input.getText().toString().toLowerCase(Locale.ROOT).trim();
      ArrayList<ResolveInfo> found=new ArrayList<>();
      for(ResolveInfo app:apps){
        String name=app.loadLabel(pm).toString().toLowerCase(Locale.ROOT);
        if(!name.contains(q))continue;
        if(selected[0]==2&&name.compareTo("n")>=0)continue;
        if(selected[0]==3&&name.compareTo("n")<0)continue;
        if(selected[0]==1&&!getPreferences(0).getBoolean("favorite_"+app.activityInfo.packageName,false))continue;
        found.add(app);
      }
      TextView count=text(found.size()+" apps",13,foreground());count.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
      add(results,count,-1,30);
      if(found.isEmpty()){
        TextView empty=text("No matching apps",17,foreground());add(results,empty,-1,140);
      }else{
        for(ResolveInfo app:found){
          LinearLayout item=horizontal();item.setPadding(dp(10),dp(8),dp(10),dp(8));
          ImageView ico=new ImageView(this);ico.setImageDrawable(app.loadIcon(pm));applyIconTint(ico);add(item,ico,44,44);
          TextView name=text(app.loadLabel(pm).toString(),16,foreground());name.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);
          LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(52),1);lp.leftMargin=dp(16);item.addView(name,lp);
          String pkg=app.activityInfo.packageName;boolean fav=getPreferences(0).getBoolean("favorite_"+pkg,false);
          TextView star=text(fav?"★":"☆",25,foreground());add(item,star,42,50);
          touch(star,()->{getPreferences(0).edit().putBoolean("favorite_"+pkg,!fav).apply();library();});
          item.setOnClickListener(v->launch(app));
          item.setOnLongClickListener(v->{Intent info=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);info.setData(android.net.Uri.parse("package:"+pkg));startActivity(info);return true;});
          LinearLayout.LayoutParams row=new LinearLayout.LayoutParams(-1,dp(64));row.bottomMargin=dp(4);results.addView(item,row);
        }
      }
    };
    for(int i=0;i<filters.length;i++){
      final int n=i;TextView choice=text(filters[i],12,foreground());setGlass(choice,16);
      LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(37),1);lp.setMargins(dp(2),0,dp(2),0);filterBar.addView(choice,lp);
      touch(choice,()->{selected[0]=n;update.run();});
    }
    input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence t,int st,int c,int a){}
      public void onTextChanged(CharSequence t,int st,int before,int count){update.run();}
      public void afterTextChanged(Editable e){}});
    update.run();enter(scroll);
  }
  private TextView button(String label,Runnable run){
    TextView v=text(label,18,foreground());setGlass(v,21);touch(v,run);return v;
  }
  private void line(String title,Runnable run){TextView b=button(title+"    ›",run);b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);b.setPadding(dp(20),0,dp(14),0);margin(shell,b,57,12);}
  private void controls(){
    canvas("controls");title("Control Centre");
    LinearLayout top=horizontal();
    TextView wifi=button("●\nWi-Fi",()->startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));top.addView(wifi,new LinearLayout.LayoutParams(0,dp(125),1));
    TextView bluetooth=button("ᛒ\nBluetooth",()->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(125),1);bp.leftMargin=dp(12);top.addView(bluetooth,bp);add(shell,top,-1,125);space(shell,12);
    line("Mobile network",()->startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)));
    TextView media=button("♪       Not Playing\n\n            ▶",()->startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS)));margin(shell,media,132,12);
    line("Focus & sound",()->startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS)));
    TextView brightness=text("☀   Brightness",19,foreground());add(shell,brightness,-1,46);
    SeekBar slider=new SeekBar(this);slider.setMax(255);slider.setProgress(155);slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
      public void onProgressChanged(SeekBar seek,int progress,boolean fromUser){if(fromUser){WindowManager.LayoutParams a=getWindow().getAttributes();a.screenBrightness=Math.max(.02f,progress/255f);getWindow().setAttributes(a);}}
      public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}
    });add(shell,slider,-1,58);
    line("Notifications",this::notifications);line("Display options",()->startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS)));line("GlassOS Settings",this::settings);enter(shell);
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
      TextView card=button((t==null?n.getPackageName():t.toString())+"\n"+(hidePrivateNotifications?"Preview hidden":(msg==null?"":msg)),()->{try{if(n.getNotification().contentIntent!=null)n.getNotification().contentIntent.send();}catch(Exception ignored){}});
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
    line("Reduce motion: "+(reduceMotion?"On":"Off"),()->{reduceMotion=!reduceMotion;getPreferences(0).edit().putBoolean("reduceMotion",reduceMotion).apply();settings();});
    line("Wallpaper: "+wallpaperNames[wallpaperStyle],this::chooseWallpaper);
    line("Customise Home Screen",this::editHome);
    line("Status clock seconds: "+(showSeconds?"On":"Off"),()->{showSeconds=!showSeconds;getPreferences(0).edit().putBoolean("showSeconds",showSeconds).apply();settings();});
    line("Icon appearance: "+tintLabels[iconTint],this::iconAppearance);
    line("Top widget: "+(widgetMode==1?"Clock":"Battery"),this::chooseWidget);
    line("Hide notification previews: "+(hidePrivateNotifications?"On":"Off"),()->{hidePrivateNotifications=!hidePrivateNotifications;getPreferences(0).edit().putBoolean("hidePrivateNotifications",hidePrivateNotifications).apply();settings();});
    line("Glass contrast: "+(highContrastGlass?"High":"Standard"),()->{
      highContrastGlass=!highContrastGlass;getPreferences(0).edit().putBoolean("highContrastGlass",highContrastGlass).apply();settings();
    });
    line("Glass opacity: "+Math.round(glassStrength*100)+"%",this::chooseGlassStrength);
    
    line("Reset Home Screen layout",this::resetLayout);
    line("App permissions",()->startActivity(new Intent(Settings.ACTION_APPLICATION_SETTINGS)));
    line("Battery settings",()->startActivity(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)));
    space(shell,18);
    TextView note=text("GlassOS 0.8  •  iOS-inspired design\n\nThird-party apps and protected Samsung system screens retain their own interface.",13,foreground());add(shell,note,-1,120);
  }
  @Override public void onBackPressed(){if(page.equals("home"))super.onBackPressed();else home();}
}