import logoImage from '../assets/logo.png';

export default function Logo() {
    return (
        <div style={{
            width: '100%',
            height: '140px',
            backgroundImage: `url(${logoImage})`,
            backgroundRepeat: 'no-repeat',
            backgroundSize: 'contain',
            backgroundPosition: 'center',
            backgroundColor: '#ffffff',
            borderBottom: '1px solid #ddd'
        }} />
    );
}
