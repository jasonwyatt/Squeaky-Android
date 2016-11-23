package com.bandcamp.squeaky.test;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

import com.bandcamp.squeaky.BlobValue;
import com.bandcamp.squeaky.Database;
import com.bandcamp.squeaky.Table;
import com.bandcamp.squeaky.util.Logger;

@RunWith(AndroidJUnit4.class)
public class BlobColumnTest {

    @Test
    public void testBlobColumn() throws Exception {
        Database db = new Database(InstrumentationRegistry.getContext(), getClass().getSimpleName());
        ImageTable t = new ImageTable();
        db.addTable(t);
        db.prepare();

        Image dialer = new Image();

        db.insert("INSERT INTO image_table (name, image) VALUES (?, ?)", "ic_dialog_dialer", dialer);

        Logger.dumpTables(db);

        Cursor c = db.query("SELECT name, image FROM image_table");
        assertThat(c.getCount()).isEqualTo(1);
        assertThat(c.getColumnCount()).isEqualTo(2);
        c.moveToNext();

        assertThat(c.getType(0)).isEqualTo(Cursor.FIELD_TYPE_STRING);
        assertThat(c.getType(1)).isEqualTo(Cursor.FIELD_TYPE_BLOB);

        byte[] blob = c.getBlob(1);
        Image fromDb = new Image(blob);

        assertThat(fromDb.getBitmap().getWidth()).isEqualTo(dialer.getBitmap().getWidth());
        assertThat(fromDb.getBitmap().getHeight()).isEqualTo(dialer.getBitmap().getHeight());
    }

    public static class ImageTable extends Table {
        @Override
        public String getName() {
            return "image_table";
        }

        @Override
        public int getVersion() {
            return 1;
        }

        @Override
        public String[] getCreateTable() {
            return new String[] {
                    "CREATE TABLE image_table (name STRING NOT NULL, image BLOB NOT NULL)"
            };
        }

        @Override
        public String[] getMigration(int nextVersion) {
            return new String[0];
        }
    }

    public static class Image implements BlobValue {
        private final Bitmap mBitmap;

        public Image() {
            mBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        }

        public Image(byte[] img) {
            ByteArrayInputStream bais = new ByteArrayInputStream(img);
            mBitmap = BitmapFactory.decodeStream(bais);
        }

        public Bitmap getBitmap() {
            return mBitmap;
        }

        @Override
        public byte[] getBytes() {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mBitmap.compress(Bitmap.CompressFormat.PNG, 10, baos);
            return baos.toByteArray();
        }
    }
}
