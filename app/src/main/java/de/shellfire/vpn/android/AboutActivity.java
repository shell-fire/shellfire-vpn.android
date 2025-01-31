package de.shellfire.vpn.android;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class AboutActivity extends AppCompatActivity {

    String TAG = AboutActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        DataRepository dataRepository = DataRepository.getInstance(this);
        dataRepository.getHelpItemList().observe(this, aboutList -> {
            if (aboutList != null && !aboutList.isEmpty()) {
                LinearLayout aboutContainer = findViewById(R.id.about_container);
                aboutContainer.removeAllViews();  // Clear any existing views

                for (HelpItem item : aboutList) {
                    // Create and configure CardView
                    CardView cardView = new CardView(AboutActivity.this);
                    LinearLayout.LayoutParams cardLayoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    cardLayoutParams.setMargins(
                            getResources().getDimensionPixelSize(R.dimen.card_margin),
                            getResources().getDimensionPixelSize(R.dimen.card_margin),
                            getResources().getDimensionPixelSize(R.dimen.card_margin),
                            getResources().getDimensionPixelSize(R.dimen.card_margin)
                    );
                    cardView.setLayoutParams(cardLayoutParams);
                    cardView.setRadius(getResources().getDimension(R.dimen.card_corner_radius));
                    cardView.setCardElevation(getResources().getDimension(R.dimen.card_elevation));
                    cardView.setUseCompatPadding(true);

                    // Create LinearLayout for CardView content
                    LinearLayout cardContent = new LinearLayout(AboutActivity.this);
                    cardContent.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    cardContent.setOrientation(LinearLayout.VERTICAL);
                    cardContent.setPadding(
                            getResources().getDimensionPixelSize(R.dimen.card_padding),
                            getResources().getDimensionPixelSize(R.dimen.card_padding),
                            getResources().getDimensionPixelSize(R.dimen.card_padding),
                            getResources().getDimensionPixelSize(R.dimen.card_padding)
                    );

                    // Create and configure header TextView
                    TextView headerView = new TextView(AboutActivity.this);
                    headerView.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    headerView.setText(item.getHeader());
                    headerView.setGravity(Gravity.CENTER_HORIZONTAL);
                    headerView.setTextColor(getResources().getColor(R.color.gray_text_color));
                    headerView.setTextSize(getResources().getDimension(R.dimen.about_txt_size_10));
                    headerView.setTypeface(null, Typeface.BOLD);

                    // Create and configure text TextView
                    TextView textView = new TextView(AboutActivity.this);
                    textView.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    textView.setText(item.getText());
                    textView.setGravity(Gravity.CENTER_HORIZONTAL);
                    textView.setTextColor(getResources().getColor(R.color.gray_text_color));

                    // Add views to the card content
                    cardContent.addView(headerView);
                    cardContent.addView(textView);

                    // Add content to the CardView
                    cardView.addView(cardContent);

                    // Add CardView to the container
                    aboutContainer.addView(cardView);

                    // Add space between sections
                    Space spaceView = new Space(AboutActivity.this);
                    spaceView.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            getResources().getDimensionPixelSize(R.dimen.about_space_height)
                    ));
                    aboutContainer.addView(spaceView);
                }

                // Add Back button at the bottom
                Button backButton = findViewById(R.id.back_btn);
                backButton.setOnClickListener(v -> finish());
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.about, menu);
        return true;
    }
}