package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Leaf implements LeafStore {

    private final static String STORE_PREF = "leafstore";
    private final static String ID_KEY = "note_id_set";
    private final static String ADDDATE = "note_date_set";
    private final static String ADDTIME = "note_time_set";
    private final static String CREATEDATE = "note_date_";
    private final static String TITLE_PREFIX = "note_title_";
    private final static String BODY_PREFIX = "note_body_";
    private final static boolean HIDE = false;

    private final Context context;

    public Leaf(Context context) {
        this.context = context;
    }

    @Override
    public List<Note> loadAll(boolean includeHidden) {
        ArrayList<Note> notes = new ArrayList<>();
        Set<String> noteIds = findAllIds();

        for (String noteId : noteIds) {
            Note note = findById(noteId);
            notes.add(note);

            // Log.d("Leaf", "Loaded Note: " + note.getTitle() + ", Hide: " + note.isHide());
            if (!note.isHide() || includeHidden) {
                notes.add(note);
            }
        }

        DateTimeFormatter d;
        DateTimeFormatter t;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                d = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY);
                t = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY);
                notes.sort(Comparator
                        .comparing((Note o) -> LocalDate.parse(o.getDate(), d))
                        .thenComparing(o -> LocalTime.parse(o.getTime(), t)));
            } catch (DateTimeParseException dateTimeParseException) {
                Log.d("dateTimeParseException", "loadAll: "+dateTimeParseException.getClass());
            }
        }

        Collections.reverse(notes);
        return notes;
    }

    private @NonNull Set<String> findAllIds() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(STORE_PREF, Context.MODE_PRIVATE);
        return findAllIds(sharedPreferences);
    }


    @SuppressLint("MutatingSharedPrefs")
    @Override
    public Note save(Note note) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(STORE_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Set<String> ids = findAllIds(sharedPreferences);
        if(note.getId() == null || note.getId().isEmpty()) {
            note.setId(Note.makeId());
        }


        if (!ids.contains(note.getId())) {
            ids.add(note.getId());
            editor.putStringSet(ID_KEY, ids);
        }



        editor.putString(TITLE_PREFIX + note.getId(), note.getTitle());
        editor.putString(BODY_PREFIX + note.getId(), note.getBody());
        editor.putString(ADDDATE + note.getId(), note.getDate());
        editor.putString(ADDTIME + note.getId(), note.getTime());
        //editor.putBoolean(HIDE + note.getId(), note.isHide());

        // Migriere alten Schlüssel, falls vorhanden
        if (sharedPreferences.contains(HIDE + note.getId())) {
            boolean oldHide = sharedPreferences.getBoolean(HIDE + note.getId(), false);
          //  Log.d("Leaf", "Migrating old hide key: " + oldHide);
            editor.remove(HIDE + note.getId()); // Alten Schlüssel entfernen
            editor.putBoolean(HIDE + "_" + note.getId(), oldHide); // Neuen Schlüssel setzen
        } else {
            editor.putBoolean(HIDE + "_" + note.getId(), note.isHide());
        }

     //   Log.d("Leaf", "Saving Note: Title = " + note.getTitle() + ", Hide = " + note.isHide());
        editor.apply();

        return note;
    }

    private static @NonNull Set<String> findAllIds(SharedPreferences sharedPreferences) {
        Set<String> ids = sharedPreferences.getStringSet(ID_KEY, null);
        if(ids == null) {
            return new HashSet<>();
        }
        return ids;
    }

    @Override
    @SuppressLint("MutatingSharedPrefs")
    public void remove(Note note) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(STORE_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Set<String> ids = findAllIds(sharedPreferences);

        if (ids.isEmpty() || !ids.contains(note.getId()))  {
            return;
        }

        ids.remove(note.getId());
        editor.remove(ID_KEY + note.getId());
        editor.remove(TITLE_PREFIX + note.getId());
        editor.remove(BODY_PREFIX + note.getId());
        editor.remove(ADDDATE + note.getId());
        editor.remove(ADDTIME + note.getId());
        editor.remove(HIDE + note.getId());
        editor.remove(HIDE + "_" + note.getId());
        editor.apply();
    }

    @Override
    public Note findById(String noteId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(STORE_PREF, Context.MODE_PRIVATE);
        String title = sharedPreferences.getString(TITLE_PREFIX + noteId, "");
        String body = sharedPreferences.getString(BODY_PREFIX + noteId, "");
        String noteDate = sharedPreferences.getString(ADDDATE+ noteId,"");
        String noteTime = sharedPreferences.getString(ADDTIME + noteId,"");
        String noteCreateDate = sharedPreferences.getString(CREATEDATE+ noteId,"");
        if (sharedPreferences.contains(HIDE + noteId)) {
            boolean noteHide = sharedPreferences.getBoolean(HIDE + noteId, false);
            sharedPreferences.edit()
                    .remove(HIDE + noteId)
                    .putBoolean(HIDE + "_" + noteId, noteHide)
                .apply();
            // Log.d("Leaf", "Migrating old hide key for noteId: " + noteId + ", Value: " + noteHide);
        }
        boolean noteHide = sharedPreferences.getBoolean(HIDE + "_" + noteId, false);

        return new Note(title, body, noteDate, noteTime, noteCreateDate, noteHide, noteId);
    }
}
