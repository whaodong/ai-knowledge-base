import { Layout as AntLayout } from 'antd'
import { Outlet } from 'react-router-dom'
import Header from './Header'
import Sidebar from './Sidebar'

const { Content } = AntLayout

const Layout = () => {
  return (
    <AntLayout className="min-h-screen">
      <Sidebar />
      <AntLayout>
        <Header />
        <Content className="p-6 bg-gray-50">
          <Outlet />
        </Content>
      </AntLayout>
    </AntLayout>
  )
}

export default Layout
