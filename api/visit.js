// Privacy-minimised view alert. Requires RESEND_API_KEY and RESEND_FROM_EMAIL
// configured as server-side Vercel environment variables.
export default async function handler(req,res){
  res.setHeader('Cache-Control','no-store');
  if(req.method!=='POST')return res.status(405).json({ok:false});
  if(req.body?.event!=='demo_open')return res.status(400).json({ok:false});
  const key=process.env.RESEND_API_KEY;
  const from=process.env.RESEND_FROM_EMAIL;
  if(!key||!from)return res.status(503).json({ok:false,reason:'email_not_configured'});
  // One alert per five-minute interval limits repeat refresh notifications.
  const slot=Math.floor(Date.now()/300000);
  try{
    const response=await fetch('https://api.resend.com/emails',{
      method:'POST',
      headers:{
        'Authorization':'Bearer '+key,
        'Content-Type':'application/json',
        'Idempotency-Key':'vapemart-demo-open-'+slot
      },
      body:JSON.stringify({
        from,
        to:['alex.promptvault@gmail.com'],
        subject:'Vape Mart demo link opened',
        text:'Someone opened your Vape Mart website demo at '+new Date().toISOString()+'.\n\nhttps://vapemart-commerce-demo.vercel.app/\n\nThis does not prove Jas personally visited. No visitor identity or IP address was collected.'
      })
    });
    if(!response.ok){
      // No user data or credentials are included in error response.
      return res.status(502).json({ok:false,reason:'delivery_failed'});
    }
    return res.status(200).json({ok:true});
  }catch(e){return res.status(502).json({ok:false,reason:'delivery_unavailable'});}
}
