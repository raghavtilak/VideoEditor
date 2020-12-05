
        package com.raghav.gfgffmpeg;

        import android.content.ContentUris;
        import android.content.Context;
        import android.database.Cursor;
        import android.graphics.Bitmap;
        import android.net.Uri;
        import android.os.Build;
        import android.os.Environment;
        import android.provider.DocumentsContract;
        import android.provider.MediaStore;
        import android.provider.OpenableColumns;

        import java.io.BufferedInputStream;
        import java.io.BufferedOutputStream;
        import java.io.File;
        import java.io.FileOutputStream;
        import java.io.InputStream;
        public class FileUtils {

            /**
             * Get a file from a Uri.
             * Framework Documents, as well as the _data field for the MediaStore and
             * other file-based ContentProviders.
             *
             * @param context The context.
             * @param uri     The Uri to query.
             */
            public static File getFileFromUri(final Context context, final Uri uri) throws Exception {

                String path = null;

                // DocumentProvider
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if (DocumentsContract.isDocumentUri(context, uri)) { // TODO: 2015. 11. 17. KITKAT


                        // ExternalStorageProvider
                        if (isExternalStorageDocument(uri)) {
                            final String docId = DocumentsContract.getDocumentId(uri);
                            final String[] split = docId.split(":");
                            final String type = split[0];


                            if ("primary".equalsIgnoreCase(type)) {
                                path = Environment.getExternalStorageDirectory() + "/" + split[1];
                            }

                            // TODO handle non-primary volumes

                        } else if (isDownloadsDocument(uri)) { // DownloadsProvider

                            final String id = DocumentsContract.getDocumentId(uri);
                            final Uri contentUri = ContentUris.withAppendedId(
                                    Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                            path = getDataColumn(context, contentUri, null, null);

                        } else if (isMediaDocument(uri)) { // MediaProvider


                            final String docId = DocumentsContract.getDocumentId(uri);
                            final String[] split = docId.split(":");
                            final String type = split[0];

                            Uri contentUri = null;
                            if ("image".equals(type)) {
                                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                            } else if ("video".equals(type)) {
                                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                            } else if ("audio".equals(type)) {
                                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                            }

                            final String selection = "_id=?";
                            final String[] selectionArgs = new String[]{
                                    split[1]
                            };

                            path = getDataColumn(context, contentUri, selection, selectionArgs);

                        }  // MediaStore (and general)
                    } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                        path = getDataColumn(context, uri, null, null);
                    }
                    // File
                    else if ("file".equalsIgnoreCase(uri.getScheme())) {
                        path = uri.getPath();
                    }

                    return new File(path);
                } else {

                    Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
                    return new File(cursor.getString(cursor.getColumnIndex("_data")));
                }

            }

            /**
             * Get the value of the data column for this Uri. This is useful for
             * MediaStore Uris, and other file-based ContentProviders.
             *
             * @param context       The context.
             * @param uri           The Uri to query.
             * @param selection     (Optional) Filter used in the query.
             * @param selectionArgs (Optional) Selection arguments used in the query.
             * @return The value of the _data column, which is typically a file path.
             */
            public static String getDataColumn(Context context, Uri uri, String selection,
                                               String[] selectionArgs) {

                Cursor cursor = null;
                final String column = MediaStore.Images.Media.DATA;
                final String[] projection = {
                        column
                };

                try {
                    cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                            null);
                    if (cursor != null && cursor.moveToFirst()) {
                        final int column_index = cursor.getColumnIndexOrThrow(column);
                        return cursor.getString(column_index);
                    }
                } finally {
                    if (cursor != null)
                        cursor.close();
                }
                return null;
            }


            /**
             * @param uri The Uri to check.
             * @return Whether the Uri authority is ExternalStorageProvider.
             */
            public static boolean isExternalStorageDocument(Uri uri) {
                return "com.android.externalstorage.documents".equals(uri.getAuthority());
            }

            /**
             * @param uri The Uri to check.
             * @return Whether the Uri authority is DownloadsProvider.
             */
            public static boolean isDownloadsDocument(Uri uri) {
                return "com.android.providers.downloads.documents".equals(uri.getAuthority());
            }

            /**
             * @param uri The Uri to check.
             * @return Whether the Uri authority is MediaProvider.
             */
            public static boolean isMediaDocument(Uri uri) {
                return "com.android.providers.media.documents".equals(uri.getAuthority());
            }
        }