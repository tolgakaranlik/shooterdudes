package com.tolka.shooterdudes;

import org.json.JSONException;

import com.gamehouse.crosspromotion.CrossPromotion;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;
import com.tolka.shooterdudes.GlRenderer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class Main extends Activity {

    private GLSurfaceView glSurfaceView;
    private GlRenderer renderer;
    private PaymentProcessor paymentProcess;

    private static PayPalConfiguration config = new PayPalConfiguration()
    .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)// ENVIRONMENT_PRODUCTION)
    .clientId("ATneqpDNJIGtD282k52-dp6tVR-G1Ofu862L54o1et7zHzMIsFlPT-mu4LH5sW-3WbfO_ivdhk6aVuCg"); // sandbox
    // .clientId("AUNvR9Io6qcHUTSAHMngE6i_n3mzei1xdTW0chetIhv_Fxw9D_f3-zDrtTHiWwTsUJOuyreib4pgFJaq"); // live
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, PayPalService.class);
	    intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
	    startService(intent);
	    
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		
		renderer = new GlRenderer(this, size.x, size.y, config);
		glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setRenderer(renderer);
		glSurfaceView.setPreserveEGLContextOnPause(true); 
        //glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        setContentView(glSurfaceView);
    }
    
	@Override
	protected void onResume() {
		super.onResume();

		CrossPromotion.instance().onResume();
		glSurfaceView.onResume();
	}
	@Override
	protected void onPause() {
		super.onPause();

		CrossPromotion.instance().onPause();
		glSurfaceView.onPause();
	}
	
	@Override
	public void onDestroy() {
	    stopService(new Intent(this, PayPalService.class));
	    super.onDestroy();
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		try {
			paymentProcess = new PaymentProcessor(null, null);
			boolean result = renderer.onTouchEvent(e, paymentProcess);

			if(paymentProcess.getAmount() != null)
			{
			    PayPalPayment payment = new PayPalPayment(paymentProcess.getAmount(),
		    		"USD", paymentProcess.getExplanation(),
		            PayPalPayment.PAYMENT_INTENT_SALE);

			    Intent intent = new Intent(this, PaymentActivity.class);

			    // send the same configuration for restart resiliency
			    intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
			    intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);

			    startActivityForResult(intent, 0);
			}
			
			return result;
		} catch (NoSuchMethodException e1) {
			e1.printStackTrace();
			return false;
		}
	}

	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
	    if (resultCode == Activity.RESULT_OK) {
	        PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
	        if (confirm != null) {
	            try {
	                String code = confirm.toJSONObject().toString(4);
	                if(code.indexOf("\"status\": \"approved\"") == -1)
	                {
	                	renderer.displayMessage(paymentProcess.getExplanation());
	                	
	                	if(paymentProcess.getExplanation().indexOf(" $10.000 In Game Money") != -1)
	                	{
	                		renderer.addMoney(10000);
	                	}
	                	
	                	if(paymentProcess.getExplanation().indexOf(" $20.000 In Game Money") != -1)
	                	{
	                		renderer.addMoney(20000);
	                	}
	                	
	                	if(paymentProcess.getExplanation().indexOf(" $50.000 In Game Money") != -1)
	                	{
	                		renderer.addMoney(50000);
	                	}
	                	
	                	if(paymentProcess.getExplanation().indexOf(" $100.000 In Game Money") != -1)
	                	{
	                		renderer.addMoney(100000);
	                	}
	                	
	                	if(paymentProcess.getExplanation().indexOf(" 10 In Game Gold") != -1)
	                	{
	                		renderer.addGold(10);
	                	}
	                	
	                	if(paymentProcess.getExplanation().indexOf(" 20 In Game Gold") != -1)
	                	{
	                		renderer.addGold(20);
	                	}
	                	
	                	if(paymentProcess.getExplanation().indexOf(" 50 In Game Gold") != -1)
	                	{
	                		renderer.addGold(50);
	                	}
	                	
	                	if(paymentProcess.getExplanation().indexOf(" 100 In Game Gold") != -1)
	                	{
	                		renderer.addGold(100);
	                	}
	                } else {
	                	renderer.displayMessage("Payment was not approved");
	                }

	            } catch (JSONException e) {
	                // an extremely unlikely failure occurred
	            }
	        }
	    }
	    else if (resultCode == Activity.RESULT_CANCELED) {
	        // The user canceled
	    	renderer.displayMessage("Payment canceled");
	    }
	    else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
	        // An invalid Payment or PayPalConfiguration was submitted. Please see the docs.
	    	renderer.displayMessage("Invalid response from payment integrator");
	    }
	}
}
