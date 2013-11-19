package com.cheruku.android.zatapona;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.datanucleus.query.JPACursorHelper;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityManager;
import javax.persistence.Query;

@Api(name = "castmediaendpoint", namespace = @ApiNamespace(ownerDomain = "cheruku.com", ownerName = "cheruku.com", packagePath = "android.zatapona"))
public class CastMediaEndpoint {

    /**
     * This method lists all the entities inserted in datastore.
     * It uses HTTP GET method and paging support.
     *
     * @return A CollectionResponse class containing the list of all entities
     * persisted and a cursor to the next page.
     */
    @SuppressWarnings({"unchecked", "unused"})
    @ApiMethod(name = "listCastMedia")
    public CollectionResponse<CastMedia> listCastMedia(
            @Nullable @Named("cursor") String cursorString,
            @Nullable @Named("limit") Integer limit) {

        EntityManager mgr = null;
        List<CastMedia> execute = null;

        try {
            mgr = getEntityManager();
            Query query = mgr.createQuery("select from CastMedia as CastMedia");
            Cursor cursor;
            if (cursorString != null && cursorString.trim().length() > 0) {
                cursor = Cursor.fromWebSafeString(cursorString);
                query.setHint(JPACursorHelper.CURSOR_HINT, cursor);
            }

            if (limit != null) {
                query.setFirstResult(0);
                query.setMaxResults(limit);
            }

            execute = (List<CastMedia>) query.getResultList();
            cursor = JPACursorHelper.getCursor(execute);
            if (cursor != null) cursorString = cursor.toWebSafeString();

            // Tight loop for fetching all entities from datastore and accomodate
            // for lazy fetch.
            for (CastMedia obj : execute) ;
        } finally {
            if (mgr != null) {
                mgr.close();
            }
        }

        return CollectionResponse.<CastMedia>builder()
                .setItems(execute)
                .setNextPageToken(cursorString)
                .build();
    }

    /**
     * This method gets the entity having primary key id. It uses HTTP GET method.
     *
     * @param id the primary key of the java bean.
     * @return The entity with primary key id.
     */
    @ApiMethod(name = "getCastMedia")
    public CastMedia getCastMedia(@Named("id") String id) {
        EntityManager mgr = getEntityManager();
        CastMedia castMedia = null;
        try {
            castMedia = mgr.find(CastMedia.class, id);
        } finally {
            mgr.close();
        }
        return castMedia;
    }

    /**
     * This inserts a new entity into App Engine datastore. If the entity already
     * exists in the datastore, an exception is thrown.
     * It uses HTTP POST method.
     *
     * @param castMedia the entity to be inserted.
     * @return The inserted entity.
     */
    @ApiMethod(name = "insertCastMedia")
    public CastMedia insertCastMedia(CastMedia castMedia) {
        EntityManager mgr = getEntityManager();
        try {
            if (containsCastMedia(castMedia)) {
                throw new EntityExistsException("Object already exists");
            }
            mgr.persist(castMedia);
        } finally {
            mgr.close();
        }
        return castMedia;
    }

    /**
     * This method is used for updating an existing entity. If the entity does not
     * exist in the datastore, an exception is thrown.
     * It uses HTTP PUT method.
     *
     * @param castMedia the entity to be updated.
     * @return The updated entity.
     */
    @ApiMethod(name = "updateCastMedia")
    public CastMedia updateCastMedia(CastMedia castMedia) {
        EntityManager mgr = getEntityManager();
        try {
            if (!containsCastMedia(castMedia)) {
                throw new EntityNotFoundException("Object does not exist");
            }
            mgr.persist(castMedia);
        } finally {
            mgr.close();
        }
        return castMedia;
    }

    /**
     * This method removes the entity with primary key id.
     * It uses HTTP DELETE method.
     *
     * @param id the primary key of the entity to be deleted.
     * @return The deleted entity.
     */
    @ApiMethod(name = "removeCastMedia")
    public CastMedia removeCastMedia(@Named("id") String id) {
        EntityManager mgr = getEntityManager();
        CastMedia castMedia = null;
        try {
            castMedia = mgr.find(CastMedia.class, id);
            mgr.remove(castMedia);
        } finally {
            mgr.close();
        }
        return castMedia;
    }

    private boolean containsCastMedia(CastMedia castMedia) {
        EntityManager mgr = getEntityManager();
        boolean contains = true;
        try {
            CastMedia item = mgr.find(CastMedia.class, castMedia.getVideoUrl());
            if (item == null) {
                contains = false;
            }
        } finally {
            mgr.close();
        }
        return contains;
    }

    private static EntityManager getEntityManager() {
        return EMF.get().createEntityManager();
    }

}
