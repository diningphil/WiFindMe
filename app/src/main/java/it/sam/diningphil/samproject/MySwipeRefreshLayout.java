package it.sam.diningphil.samproject;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;

/*
 *  Aggiunto swipeToRefresh solo per mostrare la progress bar.
 *
 *  in futuro calcola il numero di item, se l'utente può scrollare verso il basso non attivo lo swipe,
 *  altrimenti attivi il refresh . In questo modo usi lo swipetorefresh nel modo corretto
* */
public class MySwipeRefreshLayout extends SwipeRefreshLayout {

    public MySwipeRefreshLayout(Context context) {
        super(context);
    }

    public MySwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean canChildScrollUp() {
        return true;
    }
}
