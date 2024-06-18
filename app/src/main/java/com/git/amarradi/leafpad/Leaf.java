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
import java.util.Set;

public class Leaf implements LeafStore {

    private final static String STORE_PREF = "leafstore";
    private final static String ID_KEY = "note_id_set";
    private final static String ADDDATE = "note_date_set";
    private final static String ADDTIME = "note_time_set";
    private final static String CREATEDATE = "note_date_";
    private final static String TITLE_PREFIX = "note_title_";
    private final static String BODY_PREFIX = "note_body_";
    private final static String HIDE_PREFIX = "note_hide_";

    private final Context context;


    public Leaf(Context context) {
        this.context = context;
    }

    @Override
    public List<Note> loadAll() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(STORE_PREF, Context.MODE_PRIVATE);
        ArrayList<Note> notes = new ArrayList<>();
        Set<String> noteIds = findAllIds();

        for (String noteId : noteIds) {
            notes.add(findById(noteId));
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
        editor.putBoolean(HIDE_PREFIX + note.getId(), !note.isHide());
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
        editor.remove(HIDE_PREFIX + note.getId());
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
        boolean noteHide = sharedPreferences.getBoolean(HIDE_PREFIX + noteId,false);

        return new Note(title, body, noteDate, noteTime, noteCreateDate, noteHide, noteId);
    }
}
