package org.albiongames.madmadmax.power;

import com.squareup.tape.QueueFile;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

/**
 * Created by dair on 12/06/16.
 */
public class Storage
{
    QueueFile mQueueFile = null;

    public Storage(String name) throws IOException
    {
        File file = new File(name);
        mQueueFile = new QueueFile(file);
    }

    public void put(StorageEntry.Base entry)
    {
        if (entry == null)
            return;
        try
        {
            String s = entry.toString();
            byte[] data = s.getBytes("UTF-8");
            mQueueFile.add(data);

        }
        catch (Exception ex)
        {
            // some shit happened
        }
    }

    public StorageEntry.Base get()
    {
        try
        {
            byte[] data = mQueueFile.peek();
            if (data == null)
                return null;

            String s = new String(data, "UTF-8");
            JSONObject object = new JSONObject(s);
            StorageEntry.Base ret = StorageEntry.createFromJson(object);
            return ret;
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    public void remove()
    {
        try
        {
            mQueueFile.remove();
        }
        catch (IOException ex)
        {

        }
    }

    public int size()
    {
        return mQueueFile.size();
    }
}
