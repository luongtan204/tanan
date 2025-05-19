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


==================================================================
        try (Scanner sc = new Scanner(System.in)) {

            while (true) {
                System.out.println("1.  Cập nhật đơn giá cho một album theo mã");
                System.out.println("2. Tìm kiếm các album thuộc về loại nhạc và năm phát hành");
                System.out.println("3. Thống kê số album theo từng thể loại");
                System.out.println("4. Thoát chương trình");
                System.out.print("Lựa chọn công việc của bạn: ");
                int choice = Integer.parseInt(sc.nextLine());

                switch (choice) {
                    case 1 -> {
                        try {
                            System.out.print("Nhập mã album: ");
                            String albumId = sc.nextLine();
                            System.out.print("Nhập giá tiền mới: ");
                            double price = Double.parseDouble(sc.nextLine());
                            System.out.println(albumService.updatePriceOfAlbum(albumId, price));
                        } catch (Exception e) {
                            System.out.println("Kiểu dữ liệu không hợp lệ");
                        }

                    }
                    case 2 -> {
                        try {
                            System.out.print("Nhập tên thể loại: ");
                            String genreName = sc.nextLine();
                            System.out.print("Nhập năm phát hành: ");
                            int releaseYear = Integer.parseInt(sc.nextLine());
                            System.out.println(albumService.listAlbumByGenre(genreName, releaseYear));
                        } catch (Exception e) {
                            System.out.println("Kiểu dữ liệu không hợp lệ");
                        }
                    }
                    case 3 -> {
                        albumService
                                .getNumberOfAlbumsByGenre()
                                .forEach((k, v) -> System.out.println(k + ": " + v));
                    }
                    case 4 ->  System.exit(0);
                }

            }

        }



    }
}



    
