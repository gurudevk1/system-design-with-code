package org.npathai.dao;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.npathai.config.MySqlDatasourceConfiguration;
import org.npathai.model.Redirection;

import javax.annotation.Nonnull;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySqlRedirectionDao implements RedirectionDao {

    private static final String SELECT_BY_ID_SQL = "select * from redirection where id = ?";
    private static final String INSERT_SQL = "insert into redirection (id, long_url, created_at, expiry_at, uid) values (?, ?, ?, ?, ?)";
    private static final String DELETE_BY_ID_SQL = "delete from redirection where id = ?";
    private static final String SELECT_BY_USER_ID_SQL = "select * from redirection where uid = ? order by created_at desc";

    private final MysqlDataSource dataSource;

    // FIXME constructor is doing real work
    // FIXME use connection pooling
    public MySqlRedirectionDao(MySqlDatasourceConfiguration mySqlDatasourceConfiguration) {
        dataSource = new MysqlDataSource();
        dataSource.setUser(mySqlDatasourceConfiguration.getUser());
        dataSource.setPassword(mySqlDatasourceConfiguration.getPassword());
        dataSource.setUrl(mySqlDatasourceConfiguration.getUrl());
        // FIXME how to ensure DB availability when services are starting dynamically and don't have control over when
        // DB will start. Commenting this check because causes intermittent failures in docker compose
        // dataSource.getConnection().close();
    }

    @Override
    public void save(Redirection redirection) throws DataAccessException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL)) {

            preparedStatement.setString(1, redirection.id());
            preparedStatement.setString(2, redirection.longUrl());
            preparedStatement.setTimestamp(3, new Timestamp(redirection.createdAtMillis()));
            preparedStatement.setTimestamp(4, new Timestamp(redirection.expiryAtMillis()));
            preparedStatement.setString(5, redirection.uid());
            int count = preparedStatement.executeUpdate();
            assert count == 1;
        } catch (SQLException ex) {
            throw new DataAccessException(ex);
        }
    }

    @Override
    public Optional<Redirection> getById(@Nonnull String id) throws DataAccessException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = createSelectByIdStatement(connection, id);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            if (!resultSet.next()) {
                return Optional.empty();
            }

            return Optional.of(createRedirection(resultSet));
        } catch (SQLException ex) {
            throw new DataAccessException(ex);
        }
    }

    @Override
    public void deleteById(String id) throws DataAccessException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(DELETE_BY_ID_SQL)) {

            preparedStatement.setString(1, id);
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException(ex);
        }
    }

    @Override
    public List<Redirection> getAllByUser(String uid) throws DataAccessException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = createSelectByUserIdStatement(connection, uid);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            List<Redirection> redirectionsByUser = new ArrayList<>();
            while (resultSet.next()) {
                redirectionsByUser.add(createRedirection(resultSet));
            }
            return redirectionsByUser;
        } catch (SQLException ex) {
            throw new DataAccessException(ex);
        }
    }

    private PreparedStatement createSelectByUserIdStatement(Connection connection, String uid) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_BY_USER_ID_SQL);
        preparedStatement.setString(1, uid);
        return preparedStatement;
    }

    private PreparedStatement createSelectByIdStatement(Connection connection, String id) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_BY_ID_SQL);
        preparedStatement.setString(1, id);
        return preparedStatement;
    }

    private Redirection createRedirection(ResultSet resultSet) throws SQLException {
        return new Redirection(resultSet.getString("id"), resultSet.getString("long_url"),
                resultSet.getTimestamp("created_at").getTime(),
                resultSet.getTimestamp("expiry_at").getTime(),
                resultSet.getString("uid"));
    }
}
