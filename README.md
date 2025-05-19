# tanan


public class JPAUtil {
    private static final String PERSISTENCE_UNIT_NAME = "mariadb-pu";
    private static EntityManagerFactory entityManagerFactory;

    static {
        try {
            entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static EntityManager getEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    public static void close() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
    }
}


public abstract class GenericDAO <T, ID>{

    protected EntityManager em;
    protected Class<T> clazz;

    public GenericDAO(Class<T> clazz) {
        this.clazz = clazz;
        this.em = JPAUtil.getEntityManager();
    }

    public GenericDAO(EntityManager em, Class<T> clazz) {
        this.em = em;
        this.clazz = clazz;
    }

    public T findById(ID id){
        return em.find(clazz, id);
    }

    public List<T> getAll(){
        return em.createQuery("from " + clazz.getSimpleName(), clazz)
                .getResultList();
    }

    public boolean save(T t){
        EntityTransaction tr = em.getTransaction();
        try{
            tr.begin();
            em.persist(t);
            tr.commit();
            return true;
        }catch (Exception ex){
            if(tr.isActive())
                tr.rollback();
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }
    public boolean update(T t){
        EntityTransaction tr = em.getTransaction();
        try{
            tr.begin();
            em.merge(t);
            tr.commit();
            return true;
        }catch (Exception ex){
            if(tr.isActive())
                tr.rollback();
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }
    public boolean delete(ID id){
        EntityTransaction tr = em.getTransaction();
        try{
            tr.begin();
            T t = em.find(clazz, id);
            if(t != null){
                em.remove(t);
                tr.commit();
                return true;
            }
        }catch (Exception ex){
            if(tr.isActive())
                tr.rollback();
            throw new RuntimeException(ex.getMessage(), ex);
        }

        return false;
    }
}

    
