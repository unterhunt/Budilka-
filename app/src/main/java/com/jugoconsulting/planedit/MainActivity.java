package com.jugoconsulting.planedit;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
  static final int OPEN=10, EXPORT=11;
  EditorView editor; TextView pageText,status; ParcelFileDescriptor pfd; PdfRenderer renderer; Uri source; int page=0;
  final Map<Integer,ArrayList<Obj>> edits=new HashMap<>();

  @Override public void onCreate(Bundle b){super.onCreate(b);
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(35,35,35));
    LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(4,4,4,4);
    add(top,"Odpri",v->openPdfPicker()); add(top,"◀",v->showPage(page-1)); pageText=text("0 / 0");top.addView(pageText,new LinearLayout.LayoutParams(90,-2));add(top,"▶",v->showPage(page+1)); add(top,"Fit",v->editor.fit()); add(top,"Izvoz PDF",v->chooseExport());
    status=text("Odpri PDF načrt");status.setTextColor(Color.WHITE);top.addView(status,new LinearLayout.LayoutParams(0,-2,1)); root.addView(top,new LinearLayout.LayoutParams(-1,-2));
    editor=new EditorView(this);root.addView(editor,new LinearLayout.LayoutParams(-1,0,1));
    HorizontalScrollView hsv=new HorizontalScrollView(this);LinearLayout tools=new LinearLayout(this);tools.setPadding(4,4,4,4);
    tool(tools,"Premik",EditorView.Tool.SELECT);tool(tools,"Povezava",EditorView.Tool.WIRE);tool(tools,"Odstrani",EditorView.Tool.ERASE);tool(tools,"Rele",EditorView.Tool.RELAY);tool(tools,"NO",EditorView.Tool.NO);tool(tools,"NC",EditorView.Tool.NC);tool(tools,"Klema",EditorView.Tool.TERMINAL);tool(tools,"Varovalka",EditorView.Tool.FUSE);tool(tools,"Motor",EditorView.Tool.MOTOR);tool(tools,"Besedilo",EditorView.Tool.TEXT);add(tools,"Razveljavi",v->editor.undo());add(tools,"Izbriši izbran",v->editor.deleteSelected());
    hsv.addView(tools);root.addView(hsv,new LinearLayout.LayoutParams(-1,-2));setContentView(root);
    editor.listener=new EditorView.Listener(){public void needText(float x,float y){EditText e=new EditText(MainActivity.this);new AlertDialog.Builder(MainActivity.this).setTitle("Dodaj besedilo").setView(e).setPositiveButton("Dodaj",(d,w)->editor.addText(x,y,e.getText().toString())).setNegativeButton("Prekliči",null).show();}public void changed(){saveLocal();}};
  }
  TextView text(String s){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);return t;}
  void add(LinearLayout l,String s,View.OnClickListener c){Button b=new Button(this);b.setText(s);b.setOnClickListener(c);l.addView(b,new LinearLayout.LayoutParams(-2,-2));}
  void tool(LinearLayout l,String s,EditorView.Tool t){add(l,s,v->{editor.tool=t;status.setText("Orodje: "+s);});}
  void openPdfPicker(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/pdf");i.addCategory(Intent.CATEGORY_OPENABLE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,OPEN);}
  @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(c!=RESULT_OK||d==null)return;try{if(r==OPEN){source=d.getData();try{getContentResolver().takePersistableUriPermission(source,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}open(source);}else if(r==EXPORT)export(d.getData());}catch(Exception e){err(e);}}
  void open(Uri u)throws Exception{closePdf();pfd=getContentResolver().openFileDescriptor(u,"r");renderer=new PdfRenderer(pfd);page=0;edits.clear();loadLocal();showPage(0);status.setText("PDF odprt: "+renderer.getPageCount()+" strani");}
  void showPage(int i){if(renderer==null||i<0||i>=renderer.getPageCount())return;try{page=i;PdfRenderer.Page p=renderer.openPage(i);float s=Math.min(2.4f,2200f/Math.max(p.getWidth(),p.getHeight()));Bitmap b=Bitmap.createBitmap(Math.max(1,(int)(p.getWidth()*s)),Math.max(1,(int)(p.getHeight()*s)),Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);p.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);p.close();editor.setPage(b,edits.computeIfAbsent(i,k->new ArrayList<>()));pageText.setText((i+1)+" / "+renderer.getPageCount());}catch(Exception e){err(e);}}
  File saveFile(){return new File(getFilesDir(),"plan_"+(source==null?0:source.toString().hashCode())+".dat");}
  void saveLocal(){try(ObjectOutputStream o=new ObjectOutputStream(new FileOutputStream(saveFile()))){o.writeObject(edits);}catch(Exception ignored){}}
  @SuppressWarnings("unchecked") void loadLocal(){File f=saveFile();if(!f.exists())return;try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(f))){edits.putAll((Map<Integer,ArrayList<Obj>>)in.readObject());}catch(Exception ignored){}}
  void chooseExport(){if(renderer==null)return;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/pdf");i.putExtra(Intent.EXTRA_TITLE,"PlanEdit_revizija.pdf");startActivityForResult(i,EXPORT);}
  void export(Uri out){status.setText("Izvažam...");new Thread(()->{try{PdfDocument doc=new PdfDocument();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);for(int i=0;i<renderer.getPageCount();i++){PdfRenderer.Page rp=renderer.openPage(i);int w=rp.getWidth()*2,h=rp.getHeight()*2;Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);rp.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_PRINT);rp.close();Canvas c=new Canvas(b);c.scale(2f,2f);for(Obj o:edits.getOrDefault(i,new ArrayList<>()))o.draw(c,p,1f);PdfDocument.Page op=doc.startPage(new PdfDocument.PageInfo.Builder(w,h,i+1).create());op.getCanvas().drawBitmap(b,0,0,p);doc.finishPage(op);b.recycle();final int n=i+1;runOnUiThread(()->status.setText("Izvoz "+n+" / "+renderer.getPageCount()));}try(OutputStream os=getContentResolver().openOutputStream(out)){doc.writeTo(os);}doc.close();runOnUiThread(()->{status.setText("PDF izvožen");Toast.makeText(this,"Končano",Toast.LENGTH_LONG).show();});}catch(Exception e){runOnUiThread(()->err(e));}}).start();}
  void err(Exception e){status.setText("Napaka: "+e.getMessage());new AlertDialog.Builder(this).setTitle("Napaka").setMessage(e.toString()).setPositiveButton("V redu",null).show();}
  void closePdf(){try{if(renderer!=null)renderer.close();if(pfd!=null)pfd.close();}catch(Exception ignored){}renderer=null;pfd=null;}
  @Override protected void onDestroy(){closePdf();super.onDestroy();}

  public static class Obj implements Serializable {enum Type{WIRE,ERASE,RELAY,NO,NC,TERMINAL,FUSE,MOTOR,TEXT} Type type;float x1,y1,x2,y2;String text="";Obj(Type t,float a,float b,float c,float d){type=t;x1=a;y1=b;x2=c;y2=d;}
    void draw(Canvas c,Paint p,float scale){p.setStrokeWidth((type==Type.ERASE?10:1.4f)/scale);p.setStyle(Paint.Style.STROKE);p.setColor(type==Type.ERASE?Color.WHITE:Color.BLACK);float l=Math.min(x1,x2),r=Math.max(x1,x2),t=Math.min(y1,y2),b=Math.max(y1,y2);switch(type){case WIRE:case ERASE:c.drawLine(x1,y1,x2,y1,p);c.drawLine(x2,y1,x2,y2,p);break;case RELAY:c.drawRect(l,t,r,b,p);c.drawText("A1",l-18,t+8,p);c.drawText("A2",r+3,b,p);break;case NO:c.drawLine(l,(t+b)/2,l+12,(t+b)/2,p);c.drawLine(r-12,(t+b)/2,r,(t+b)/2,p);c.drawLine(l+12,(t+b)/2,r-12,t+3,p);break;case NC:c.drawLine(l,(t+b)/2,l+12,(t+b)/2,p);c.drawLine(r-12,(t+b)/2,r,(t+b)/2,p);c.drawLine(l+12,t+3,r-12,(t+b)/2,p);break;case TERMINAL:c.drawCircle((l+r)/2,(t+b)/2,Math.max(8,(r-l)/3),p);break;case FUSE:c.drawRect(l,t,r,b,p);c.drawLine(l,t,r,b,p);break;case MOTOR:c.drawCircle((l+r)/2,(t+b)/2,Math.max(12,Math.min(r-l,b-t)/2),p);p.setTextSize(16);c.drawText("M",(l+r)/2-6,(t+b)/2+6,p);break;case TEXT:p.setStyle(Paint.Style.FILL);p.setTextSize(14);c.drawText(text,x1,y1,p);break;}}
    boolean hit(float x,float y){float l=Math.min(x1,x2)-15,r=Math.max(x1,x2)+15,t=Math.min(y1,y2)-15,b=Math.max(y1,y2)+15;return x>=l&&x<=r&&y>=t&&y<=b;}
  }

  public static class EditorView extends View {enum Tool{SELECT,WIRE,ERASE,RELAY,NO,NC,TERMINAL,FUSE,MOTOR,TEXT} interface Listener{void needText(float x,float y);void changed();} Listener listener;Tool tool=Tool.SELECT;Bitmap page;ArrayList<Obj> objects=new ArrayList<>(),history=new ArrayList<>();Obj preview,selected;Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);Matrix m=new Matrix(),inv=new Matrix();float scale=1,tx,ty,lastX,lastY;ScaleGestureDetector scaler;
    EditorView(Context c){super(c);setBackgroundColor(Color.DKGRAY);scaler=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){public boolean onScale(ScaleGestureDetector d){float old=scale;scale=Math.max(.2f,Math.min(8,scale*d.getScaleFactor()));float f=scale/old;tx=d.getFocusX()-(d.getFocusX()-tx)*f;ty=d.getFocusY()-(d.getFocusY()-ty)*f;invalidate();return true;}});}
    void setPage(Bitmap b,ArrayList<Obj> o){page=b;objects=o;selected=null;post(this::fit);}
    void fit(){if(page==null||getWidth()==0)return;scale=Math.min((float)getWidth()/page.getWidth(),(float)getHeight()/page.getHeight());tx=(getWidth()-page.getWidth()*scale)/2;ty=(getHeight()-page.getHeight()*scale)/2;invalidate();}
    void matrix(){m.reset();m.postScale(scale,scale);m.postTranslate(tx,ty);m.invert(inv);}PointF doc(float x,float y){matrix();float[] a={x,y};inv.mapPoints(a);return new PointF(Math.round(a[0]/5)*5,Math.round(a[1]/5)*5);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);if(page==null)return;matrix();c.save();c.concat(m);c.drawBitmap(page,0,0,paint);for(Obj o:objects)o.draw(c,paint,scale);if(preview!=null)preview.draw(c,paint,scale);if(selected!=null){paint.setColor(Color.BLUE);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2/scale);c.drawRect(Math.min(selected.x1,selected.x2)-8,Math.min(selected.y1,selected.y2)-8,Math.max(selected.x1,selected.x2)+8,Math.max(selected.y1,selected.y2)+8,paint);}c.restore();}
    @Override public boolean onTouchEvent(android.view.MotionEvent e){scaler.onTouchEvent(e);if(page==null||scaler.isInProgress())return true;PointF p=doc(e.getX(),e.getY());switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:lastX=e.getX();lastY=e.getY();if(tool==Tool.SELECT){selected=find(p.x,p.y);}else if(tool==Tool.TEXT){if(listener!=null)listener.needText(p.x,p.y);}else preview=new Obj(type(tool),p.x,p.y,p.x,p.y);invalidate();return true;case MotionEvent.ACTION_MOVE:if(tool==Tool.SELECT){if(selected!=null){float dx=(e.getX()-lastX)/scale,dy=(e.getY()-lastY)/scale;selected.x1+=dx;selected.x2+=dx;selected.y1+=dy;selected.y2+=dy;}else{tx+=e.getX()-lastX;ty+=e.getY()-lastY;}lastX=e.getX();lastY=e.getY();}else if(preview!=null){preview.x2=p.x;preview.y2=p.y;}invalidate();return true;case MotionEvent.ACTION_UP:if(preview!=null){if(preview.type!=Obj.Type.WIRE&&preview.type!=Obj.Type.ERASE&&Math.abs(preview.x2-preview.x1)<12){preview.x2=preview.x1+45;preview.y2=preview.y1+30;}objects.add(preview);history.add(preview);preview=null;if(listener!=null)listener.changed();}else if(selected!=null&&listener!=null)listener.changed();invalidate();return true;}return true;}
    Obj find(float x,float y){for(int i=objects.size()-1;i>=0;i--)if(objects.get(i).hit(x,y))return objects.get(i);return null;}Obj.Type type(Tool t){return Obj.Type.valueOf(t.name());}
    void addText(float x,float y,String s){Obj o=new Obj(Obj.Type.TEXT,x,y,x+10,y+10);o.text=s;objects.add(o);history.add(o);invalidate();if(listener!=null)listener.changed();}
    void deleteSelected(){if(selected!=null){objects.remove(selected);selected=null;invalidate();if(listener!=null)listener.changed();}}
    void undo(){if(!history.isEmpty()){Obj o=history.remove(history.size()-1);objects.remove(o);invalidate();if(listener!=null)listener.changed();}}
  }
}
