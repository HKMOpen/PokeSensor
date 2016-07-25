package com.logickllc.pokemapper;

import android.support.v7.app.AppCompatActivity;

public class HelpActivity extends AppCompatActivity {
    /*public TourGuide mTutorialHandler;
    public Activity mActivity;
    public static final String STATUS_BAR = "status_bar";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        *//* Get parameters from main activity *//*
        Intent intent = getIntent();
        boolean status_bar = intent.getBooleanExtra(STATUS_BAR, false);
        if (!status_bar) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        super.onCreate(savedInstanceState);
        mActivity = this;

        setContentView(R.layout.activity_help);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu;
        getMenuInflater().inflate(R.menu.menu, menu);

        ArrayList<ImageView> buttons = new ArrayList<ImageView>();
        buttons.add(setupButton(menu.findItem(R.id.action_scan), R.drawable.ic_action_scan));
        buttons.add(setupButton(menu.findItem(R.id.action_tuner), R.drawable.ic_action_tuner));
        buttons.add(setupButton(menu.findItem(R.id.action_search), R.drawable.ic_action_search));
        
        ArrayList<MenuItem> menuItems = new ArrayList<MenuItem>();
        menuItems.add(menu.findItem(R.id.action_scan));
        menuItems.add(menu.findItem(R.id.action_tuner));
        menuItems.add(menu.findItem(R.id.action_search));
        
        ArrayList<String> titles = new ArrayList<String>();
        titles.add(getResources().getString(R.string.scanTitle));
        titles.add(getResources().getString(R.string.scanDetailsTitle));
        titles.add(getResources().getString(R.string.searchTitle));

        ArrayList<String> messages = new ArrayList<String>();
        messages.add(getResources().getString(R.string.scanMessage));
        messages.add(getResources().getString(R.string.scanDetailsMessage));
        messages.add(getResources().getString(R.string.searchMessage));

        setUpButtonTutorial(menuItems, buttons, titles, messages);
        return true;
    }

    public ImageView setupButton(MenuItem menuItem, int drawableID) {
        ImageView button = (ImageView) menuItem.getActionView();

        // just adding some padding to look better
        float density = mActivity.getResources().getDisplayMetrics().density;
        int padding = (int) (5 * density);
        button.setPadding(padding, padding, padding, padding);

        // set an image
        button.setImageDrawable(mActivity.getResources().getDrawable(drawableID));

        return button;
    }

    public void setUpButtonTutorial(final ArrayList<MenuItem> icons, final ArrayList<ImageView> buttons, final ArrayList<String> titles, final ArrayList<String> messages) {
        for (int n = 0; n < icons.size(); n++) {
            MenuItem menuItem = icons.get(n);

            if (n == 0) {
                ToolTip toolTip = new ToolTip()
                        .setTitle(titles.get(n))
                        .setDescription(messages.get(n))
                        .setGravity(Gravity.LEFT | Gravity.BOTTOM);

                mTutorialHandler = TourGuide.init(this).with(TourGuide.Technique.Click)
                        .motionType(TourGuide.MotionType.ClickOnly)
                        .setPointer(new Pointer())
                        .setToolTip(toolTip)
                        .setOverlay(new Overlay())
                        .playOn(buttons.get(n));
            }

            final int index = n;

            buttons.get(n).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mTutorialHandler.cleanUp();
                    if (index < icons.size() - 1) mTutorialHandler.setToolTip(new ToolTip().setTitle(titles.get(index + 1)).setDescription(messages.get(index + 1)).setGravity(Gravity.BOTTOM|Gravity.LEFT)).playOn(buttons.get(index + 1));
                }
            });
        }
    }*/
}
